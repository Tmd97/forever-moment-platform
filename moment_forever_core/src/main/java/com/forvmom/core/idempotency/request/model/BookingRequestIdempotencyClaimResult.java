package com.forvmom.core.idempotency.request.model;

/**
 * Union-like result containing either a new execution claim or a stored replay.
 */
public record BookingRequestIdempotencyClaimResult(
        BookingRequestIdempotencyClaim claim,
        BookingInitiationResult replay
) {
    /**
     * Creates a result granting the caller ownership of request execution.
     */
    public static BookingRequestIdempotencyClaimResult claimed(
            BookingRequestIdempotencyClaim claim
    ) {
        return new BookingRequestIdempotencyClaimResult(claim, null);
    }

    /**
     * Creates a result containing the completed request's stable response.
     */
    public static BookingRequestIdempotencyClaimResult replay(
            BookingInitiationResult replay
    ) {
        return new BookingRequestIdempotencyClaimResult(null, replay);
    }

    /**
     * Indicates whether this result contains a replay instead of a claim.
     */
    public boolean isReplay() {
        return replay != null;
    }
}
