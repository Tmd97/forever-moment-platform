package com.forvmom.core.idempotency.request.model;

/**
 * Ownership proof for one durable HTTP request-idempotency execution lease.
 */
public record BookingRequestIdempotencyClaim(
        String operation,
        String callerId,
        String idempotencyKeyHash,
        String requestFingerprint,
        String ownerToken
) {
}
