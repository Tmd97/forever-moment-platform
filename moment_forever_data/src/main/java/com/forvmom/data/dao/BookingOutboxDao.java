package com.forvmom.data.dao;

import com.forvmom.data.entities.BookingOutbox;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingOutboxDao extends GenericDao<BookingOutbox, Long> {

    /**
     * Loads an outbox record without acquiring a database lock.
     */
    BookingOutbox findByBookingReferenceId(String bookingReferenceId);

    /**
     * Loads an outbox record under a pessimistic write lock for final state revalidation.
     */
    BookingOutbox findForUpdate(String bookingReferenceId);

    /**
     * Returns outbox records eligible for retry by the scheduled poller.
     * Picks up records that are PENDING or FAILED, older than {@code olderThan},
     * and have not yet exceeded {@code maxRetries}.
     */
    List<BookingOutbox> findRetryable(LocalDateTime olderThan, int maxRetries);

    /**
     * Returns unresolved records old enough for retry or terminal compensation.
     */
    List<BookingOutbox> findUnresolved(LocalDateTime olderThan);

    /**
     * Clean up published messages that are older than a cutoff time.
     */
    int deletePublishedOlderThan(LocalDateTime cutoff);

    /**
     * Atomically claims the record by setting its status, processing start time,
     * and attempt-specific owner token if it is currently PENDING or FAILED.
     * Returns the number of rows updated (0 if already processing/published, 1 if
     * claimed).
     */
    int markAsProcessing(
            String bookingReferenceId,
            String ownerToken,
            LocalDateTime processingStartedAt);

    /**
     * Marks the matching owned processing attempt published.
     *
     * @return the affected row count; zero indicates a stale owner token
     */
    int markPublished(
            String bookingReferenceId,
            String ownerToken,
            LocalDateTime publishedAt);

    /**
     * Marks the matching owned processing attempt failed and counts the attempt.
     *
     * @return the affected row count; zero indicates a stale owner token
     */
    int markFailed(
            String bookingReferenceId,
            String ownerToken,
            String failureReason);

    /**
     * Marks an unowned unresolved record compensated after retries are exhausted.
     */
    int markCompensated(
            String bookingReferenceId,
            int minimumRetryCount,
            LocalDateTime compensatedAt);

    /**
     * Marks an unowned unresolved record dead after retries are exhausted.
     */
    int markDead(
            String bookingReferenceId,
            int minimumRetryCount,
            String failureReason);

    /**
     * Resets records whose processing start time is older than the cutoff back to
     * FAILED and counts the abandoned attempt.
     */
    int resetStuckProcessing(LocalDateTime cutoff);
}
