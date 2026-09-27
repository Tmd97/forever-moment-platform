package com.forvmom.data.dao;

import com.forvmom.data.entities.BookingRequestIdempotency;

import java.time.LocalDateTime;

public interface BookingRequestIdempotencyDao {

    /**
     * Inserts a new in-progress request unless the scoped key already exists.
     *
     * @return one when inserted, otherwise zero
     */
    int claim(
            String operation,
            String callerId,
            String idempotencyKeyHash,
            String requestFingerprint,
            String ownerToken,
            LocalDateTime leaseExpiresAt);

    /**
     * Loads the request associated with the operation, caller, and hashed key.
     */
    BookingRequestIdempotency find(String operation, String callerId, String idempotencyKeyHash);

    /**
     * Loads and pessimistically locks a request for atomic completion.
     */
    BookingRequestIdempotency findForUpdate(
            String operation,
            String callerId,
            String idempotencyKeyHash);

    /**
     * Atomically takes ownership of a matching failed or expired request.
     *
     * @return one when reclaimed, otherwise zero
     */
    int reclaim(
            String operation,
            String callerId,
            String idempotencyKeyHash,
            String requestFingerprint,
            String ownerToken,
            LocalDateTime leaseExpiresAt,
            LocalDateTime now);

    /**
     * Marks only the currently owned in-progress request failed.
     *
     * @return one when updated, otherwise zero
     */
    int markFailed(
            String operation,
            String callerId,
            String idempotencyKeyHash,
            String ownerToken,
            String failureReason,
            LocalDateTime failedAt);

    /**
     * Deletes completed replay records older than the retention cutoff.
     */
    int deleteCompletedOlderThan(LocalDateTime cutoff);
}
