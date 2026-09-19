
package com.forvmom.core.retries.service;
import com.forvmom.core.retries.model.BookingOutboxProcessingClaim;
import com.forvmom.data.dao.BookingOutboxDao;
import com.forvmom.data.entities.BookingOutbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Claims an outbox row and changes the state of that owned attempt.
 *
 * <p>Each attempt gets a new token. An old worker cannot use its token after
 * Quartz resets the row and another worker claims it.
 */
@Service
public class BookingOutboxStateService {

    private static final Logger logger =
            LoggerFactory.getLogger(BookingOutboxStateService.class);

    private final BookingOutboxDao bookingOutboxDao;

    public BookingOutboxStateService(BookingOutboxDao bookingOutboxDao) {
        this.bookingOutboxDao = bookingOutboxDao;
    }

    /**
     * Moves one pending/failed row to {@code PROCESSING} with a new token.
     *
     * @return the claim when this worker won ownership, otherwise {@code null}
     */
    @Transactional
    public BookingOutboxProcessingClaim claimForProcessing(String bookingReferenceId) {
        // A fresh token identifies only this processing attempt.
        String ownerToken = UUID.randomUUID().toString();
        int updatedCount = bookingOutboxDao.markAsProcessing(
                bookingReferenceId,
                ownerToken,
                LocalDateTime.now());
        if (updatedCount != 1) {
            logger.debug(
                    "Outbox processing claim rejected: bookingReferenceId={}",
                    bookingReferenceId);
            return null;
        }
        logger.debug(
                "Outbox processing claim acquired: bookingReferenceId={}",
                bookingReferenceId);
        return new BookingOutboxProcessingClaim(bookingReferenceId, ownerToken);
    }

    /**
     * Loads the row after it has been claimed.
     *
     * @throws IllegalStateException when the referenced outbox record does not exist
     */
    @Transactional(readOnly = true)
    public BookingOutbox loadRequired(String bookingReferenceId) {
        BookingOutbox outboxRecord =
                bookingOutboxDao.findByBookingReferenceId(bookingReferenceId);

        if (outboxRecord == null) {
            throw new IllegalStateException("Booking outbox not found: " + bookingReferenceId);
        }

        return outboxRecord;
    }

    /**
     * Marks this attempt failed only when its token still matches.
     *
     * @return {@code false} when the token is stale and no row was changed
     */
    @Transactional
    public boolean markFailed(BookingOutboxProcessingClaim claim, String reason) {
        int updatedCount = bookingOutboxDao.markFailed(
                claim.bookingReferenceId(),
                claim.ownerToken(),
                reason == null ? null : trim(reason));
        if (updatedCount != 1) {
            logger.warn(
                    "Ignored outbox failure transition for stale owner: bookingReferenceId={}",
                    claim.bookingReferenceId());
            return false;
        }
        logger.debug(
                "Outbox processing attempt marked FAILED: bookingReferenceId={}",
                claim.bookingReferenceId());
        return true;
    }

    /**
     * Fits persisted diagnostic text within the outbox column limit.
     */
    private String trim(String value) {
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}