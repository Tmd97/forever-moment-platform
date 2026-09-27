package com.forvmom.core.retries.service;

import com.forvmom.common.dto.events.BookingRequestEvent;
import com.forvmom.core.producer.BookingEventProducer;
import com.forvmom.core.retries.model.BookingOutboxProcessingClaim;
import com.forvmom.data.dao.BookingOutboxDao;
import com.forvmom.data.entities.BookingOutbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Performs the final token check and Kafka publish in one transaction.
 *
 * <p>The row stays locked until Kafka acknowledges the event. Quartz cannot
 * reset it during that time.
 */
@Service
public class BookingOutboxPublicationTransactionService {

    private static final Logger logger =
            LoggerFactory.getLogger(BookingOutboxPublicationTransactionService.class);

    private final BookingOutboxDao bookingOutboxDao;
    private final BookingEventProducer bookingEventProducer;

    public BookingOutboxPublicationTransactionService(
            BookingOutboxDao bookingOutboxDao,
            BookingEventProducer bookingEventProducer
    ) {
        this.bookingOutboxDao = bookingOutboxDao;
        this.bookingEventProducer = bookingEventProducer;
    }

    /**
     * Publishes only when the row is processing with this worker's token.
     *
     * @return {@code false} when recovery already expired or replaced the claim
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 45)
    public boolean publish(
            BookingOutboxProcessingClaim claim,
            BookingRequestEvent event
    ) {
        // Keep the row locked from this token check through the Kafka acknowledgment.
        BookingOutbox outboxRecord =
                bookingOutboxDao.findForUpdate(claim.bookingReferenceId());
        if (outboxRecord == null) {
            throw new IllegalStateException(
                    "Booking outbox not found: " + claim.bookingReferenceId());
        }
        if (!BookingOutbox.STATUS_PROCESSING.equals(outboxRecord.getStatus())
                || !claim.ownerToken().equals(outboxRecord.getProcessingOwnerToken())) {
            logger.debug(
                    "Skipped publication for stale outbox owner: bookingReferenceId={}",
                    claim.bookingReferenceId());
            return false;
        }

        // Wait for Kafka before marking the outbox row published.
        bookingEventProducer.sendBookingRequested(event).join();
        int updatedCount = bookingOutboxDao.markPublished(
                claim.bookingReferenceId(),
                claim.ownerToken(),
                LocalDateTime.now());
        if (updatedCount != 1) {
            logger.error(
                    "Outbox ownership changed while row was locked for publication: bookingReferenceId={}",
                    claim.bookingReferenceId());
            throw new IllegalStateException(
                    "Booking outbox ownership was lost during publication: "
                            + claim.bookingReferenceId());
        }
        return true;
    }
}
