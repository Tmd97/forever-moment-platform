package com.forvmom.core.retries.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forvmom.core.event_enrichment.*;
import com.forvmom.core.retries.model.BookingOutboxCompensationResult;
import com.forvmom.core.retries.model.BookingOutboxRecoveryBatch;
import com.forvmom.data.dao.BookingOutboxDao;
import com.forvmom.data.dao.BookingRequestIdempotencyDao;
import com.forvmom.data.entities.BookingOutbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runs outbox retry, compensation, and cleanup work for the Quartz jobs.
 *
 * <p>Every minute: reset timed-out workers, retry failed rows, then compensate
 * rows that reached the retry limit. Every day: remove old completed rows.
 */
@Service
public class BookingReliabilityMaintenanceService {

    private static final Logger logger =
            LoggerFactory.getLogger(BookingReliabilityMaintenanceService.class);

    @Autowired
    private BookingOutboxDao bookingOutboxDao;

    @Autowired
    private BookingRequestIdempotencyDao bookingRequestIdempotencyDao;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingOutboxCompensationTransactionService compensationTransactionService;

    @Autowired
    private BookingOutboxRecoveryTransactionService recoveryTransactionService;

    @Autowired
    private BookingEnrichmentTask bookingEnrichmentTask;

    /** Retention window for successfully published records. */
    private static final int OUTBOX_RETENTION_HOURS = 24;

    /** Replay window for completed HTTP booking requests. */
    private static final int REQUEST_IDEMPOTENCY_RETENTION_DAYS = 30;

    /**
     * Failed publish attempts allowed before compensation.
     * At retry count 5, the job releases capacity instead of starting attempt 6.
     */
    private static final int MAX_ENRICHMENT_RETRIES = 5;

    /**
     * Gives the first async publish time to finish before Quartz retries the row.
     */
    private static final long ENRICHMENT_GRACE_PERIOD_MINUTES = 2;

    /**
     * Deletes published outbox rows after 24 hours and replay records after 30 days.
     */
    @Transactional
    public void cleanupExpiredRecords() {
        LocalDateTime outboxCutoff =
                LocalDateTime.now().minusHours(OUTBOX_RETENTION_HOURS);
        int deletedOutboxCount = bookingOutboxDao.deletePublishedOlderThan(outboxCutoff);
        LocalDateTime idempotencyCutoff =
                LocalDateTime.now().minusDays(REQUEST_IDEMPOTENCY_RETENTION_DAYS);
        int deletedRequestCount =
                bookingRequestIdempotencyDao.deleteCompletedOlderThan(idempotencyCutoff);

        if (deletedOutboxCount > 0) {
            logger.info("Cleaned up {} published outbox record(s) older than {}",
                    deletedOutboxCount, outboxCutoff);
        }
        if (deletedRequestCount > 0) {
            logger.info("Cleaned up {} completed booking idempotency record(s) older than {}",
                    deletedRequestCount, idempotencyCutoff);
        }
    }

    /**
     * Resets workers running over five minutes, retries eligible rows, and
     * compensates rows with five failed attempts.
     */
    public void recoverUnresolvedOutboxRecords() {
        // Stage 1: expire abandoned PROCESSING tokens and load unresolved rows.
        LocalDateTime stuckCutoff = LocalDateTime.now().minusMinutes(5);
        LocalDateTime cutoff =
                LocalDateTime.now().minusMinutes(ENRICHMENT_GRACE_PERIOD_MINUTES);
        BookingOutboxRecoveryBatch recoveryBatch =
                recoveryTransactionService.prepareRecovery(stuckCutoff, cutoff);
        if (recoveryBatch.resetCount() > 0) {
            logger.warn(
                    "Reset {} stuck outbox record(s) from PROCESSING back to FAILED",
                    recoveryBatch.resetCount());
        }
        List<BookingOutbox> unresolvedRecords = recoveryBatch.unresolvedRecords();
        logger.info("Outbox poller found {} unresolved record(s)", unresolvedRecords.size());
        List<String> bookingReferenceIdsForRetry = new ArrayList<>();
        for (BookingOutbox outboxRecord : unresolvedRecords) {

            // Stage 2: stop retrying at the limit and release reserved capacity.
            if (outboxRecord.getRetryCount() >= MAX_ENRICHMENT_RETRIES) {
                logger.error(
                        "Booking permanently failed after {} retries: bookingReferenceId={}",
                        MAX_ENRICHMENT_RETRIES,
                        outboxRecord.getBookingReferenceId());
                compensatePermanentlyFailedBooking(outboxRecord);
            } else {
                logger.info("Retrying enrichment for bookingReferenceId={}, retryCount={}",
                        outboxRecord.getBookingReferenceId(), outboxRecord.getRetryCount());
                bookingReferenceIdsForRetry.add(outboxRecord.getBookingReferenceId());
            }
        }

        // Stage 3: dispatch retries only after the recovery transaction committed.
        bookingReferenceIdsForRetry.forEach(bookingEnrichmentTask::enrich);
    }

    /**
     * Releases capacity for a booking that can no longer be published.
     * Invalid payload or compensation failure moves the row to {@code DEAD}.
     *
     * @param outboxRecord the permanently failed outbox record to compensate
     */
    public void compensatePermanentlyFailedBooking(BookingOutbox outboxRecord) {
        String bookingReferenceId = outboxRecord.getBookingReferenceId();
        try {
            // Parse the minimal payload to get slotMapperId and guestCount
            Map<String, Object> payload = objectMapper.readValue(
                    outboxRecord.getPayload(), new TypeReference<Map<String, Object>>() {
                    });

            Long slotMapperId = toLong(payload.get("slotMapperId"));
            Integer guestCount = toInt(payload.get("guestCount"));
            LocalDate bookingDate = toLocalDate(payload.get("bookingDate"));

            if (slotMapperId == null || guestCount == null || bookingDate == null) {
                logger.error("Invalid payload for compensation: bookingReferenceId={}",
                        bookingReferenceId);
                boolean markedDead = compensationTransactionService.markDead(
                        bookingReferenceId,
                        "Invalid payload for compensation",
                        MAX_ENRICHMENT_RETRIES);
                if (!markedDead) {
                    logger.info(
                            "Skipped DEAD transition because outbox state changed: bookingReferenceId={}",
                            bookingReferenceId);
                }
                return;
            }

            BookingOutboxCompensationResult compensationResult =
                    compensationTransactionService.compensate(
                            bookingReferenceId,
                            slotMapperId,
                            bookingDate,
                            guestCount,
                            MAX_ENRICHMENT_RETRIES);
            if (!compensationResult.applied()) {
                logger.info(
                        "Skipped compensation because outbox state changed: bookingReferenceId={}",
                        bookingReferenceId);
                return;
            }
            logger.info(
                    "Booking reservation compensation handled: bookingReferenceId={}, released={}",
                    bookingReferenceId,
                    compensationResult.released());

        } catch (Exception exception) {
            logger.error(
                    "Failed to compensate booking: {}",
                    bookingReferenceId,
                    exception);

            boolean markedDead = compensationTransactionService.markDead(
                    bookingReferenceId,
                    exception.getMessage(),
                    MAX_ENRICHMENT_RETRIES);

            if (markedDead) {
                sendAlert(
                        "Compensation failed for booking: "
                                + bookingReferenceId);
            } else {
                logger.info(
                        "Skipped DEAD transition because outbox state changed: bookingReferenceId={}",
                        bookingReferenceId);
            }
        }
    }

    /**
     * Converts a numeric or textual payload value to a long identifier.
     */
    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }

    /**
     * Converts a numeric or textual payload value to an integer count.
     */
    private Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }

    /**
     * Parses the date retained in the minimal outbox payload.
     */
    private LocalDate toLocalDate(Object value) {
        return value == null ? null : LocalDate.parse(value.toString());
    }

    /**
     * Emits an operations alert. Currently log-based — the {@code ALERT:} marker is
     * the hook monitoring should watch until a real notification channel (email,
     * Slack, PagerDuty) is wired in.
     *
     * @param message human-readable description of what needs investigating
     */
    private void sendAlert(String message) {
        logger.error("ALERT: {}", message);
    }
}
