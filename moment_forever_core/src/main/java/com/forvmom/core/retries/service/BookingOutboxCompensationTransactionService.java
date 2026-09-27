package com.forvmom.core.retries.service;

import com.forvmom.core.retries.model.BookingOutboxCompensationResult;
import com.forvmom.core.services.BookingReservationService;
import com.forvmom.data.dao.BookingOutboxDao;
import com.forvmom.data.entities.BookingOutbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Finishes bookings that exhausted their outbox retry limit.
 *
 * <p>The row is locked and checked again. Compensation is skipped if a new
 * worker already claimed it.
 */
@Service
public class BookingOutboxCompensationTransactionService {

    private static final Logger logger =
            LoggerFactory.getLogger(BookingOutboxCompensationTransactionService.class);

    private final BookingOutboxDao bookingOutboxDao;
    private final BookingReservationService bookingReservationService;

    public BookingOutboxCompensationTransactionService(
            BookingOutboxDao bookingOutboxDao,
            BookingReservationService bookingReservationService
    ) {
        this.bookingOutboxDao = bookingOutboxDao;
        this.bookingReservationService = bookingReservationService;
    }

    /**
     * Releases reserved capacity and marks the row {@code COMPENSATED} together.
     *
     * @return whether compensation was applied and whether inventory was released
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BookingOutboxCompensationResult compensate(
            String bookingReferenceId,
            Long slotMapperId,
            LocalDate bookingDate,
            Integer guestCount,
            int minimumRetryCount
    ) {
        // Lock before checking retries so a new worker cannot claim at the same time.
        BookingOutbox outboxRecord =
                bookingOutboxDao.findForUpdate(bookingReferenceId);
        if (outboxRecord == null) {
            throw new IllegalStateException("Booking outbox not found: " + bookingReferenceId);
        }
        if (!isEligibleForCompensation(outboxRecord, minimumRetryCount)) {
            logger.debug(
                    "Skipped outbox compensation after locked state revalidation: bookingReferenceId={}, status={}, retryCount={}",
                    bookingReferenceId,
                    outboxRecord.getStatus(),
                    outboxRecord.getRetryCount());
            return BookingOutboxCompensationResult.skipped();
        }

        boolean released = bookingReservationService.releaseOnce(
                bookingReferenceId,
                slotMapperId,
                bookingDate,
                guestCount);
        int updatedCount = bookingOutboxDao.markCompensated(
                bookingReferenceId,
                minimumRetryCount,
                LocalDateTime.now());
        if (updatedCount != 1) {
            logger.error(
                    "Outbox compensation transition lost ownership: bookingReferenceId={}",
                    bookingReferenceId);
            throw new IllegalStateException(
                    "Booking outbox compensation ownership was lost: "
                            + bookingReferenceId);
        }
        return BookingOutboxCompensationResult.applied(released);
    }

    /**
     * Marks the row {@code DEAD} when compensation itself fails.
     *
     * @return {@code false} when the locked row is no longer eligible
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markDead(
            String bookingReferenceId,
            String failureReason,
            int minimumRetryCount
    ) {
        BookingOutbox outboxRecord =
                bookingOutboxDao.findForUpdate(bookingReferenceId);
        if (outboxRecord == null) {
            throw new IllegalStateException("Booking outbox not found: " + bookingReferenceId);
        }
        if (!isEligibleForCompensation(outboxRecord, minimumRetryCount)) {
            logger.debug(
                    "Skipped DEAD transition after locked state revalidation: bookingReferenceId={}, status={}, retryCount={}",
                    bookingReferenceId,
                    outboxRecord.getStatus(),
                    outboxRecord.getRetryCount());
            return false;
        }
        int updatedCount = bookingOutboxDao.markDead(
                bookingReferenceId,
                minimumRetryCount,
                trim(failureReason));
        if (updatedCount != 1) {
            logger.error(
                    "Outbox DEAD transition lost ownership: bookingReferenceId={}",
                    bookingReferenceId);
            throw new IllegalStateException(
                    "Booking outbox terminal ownership was lost: "
                            + bookingReferenceId);
        }
        return true;
    }

    /**
     * Checks the terminal-transition preconditions while the outbox row is locked.
     */
    private boolean isEligibleForCompensation(
            BookingOutbox outboxRecord,
            int minimumRetryCount
    ) {
        boolean unresolved =
                BookingOutbox.STATUS_PENDING.equals(outboxRecord.getStatus())
                        || BookingOutbox.STATUS_FAILED.equals(outboxRecord.getStatus());
        return unresolved
                && outboxRecord.getRetryCount() >= minimumRetryCount
                && outboxRecord.getProcessingOwnerToken() == null;
    }

    /**
     * Fits persisted diagnostic text within the outbox column limit.
     */
    private String trim(String failureReason) {
        if (failureReason == null || failureReason.length() <= 1000) {
            return failureReason;
        }
        return failureReason.substring(0, 1000);
    }
}
