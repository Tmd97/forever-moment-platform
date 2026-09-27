package com.forvmom.core.retries.model;

/**
 * Attempt-specific ownership proof for fenced outbox state transitions.
 */
public record BookingOutboxProcessingClaim(
        String bookingReferenceId,
        String ownerToken
) {
}
