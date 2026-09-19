package com.forvmom.core.idempotency.request.model;

/**
 * Stable HTTP booking-creation result returned for both first execution and replay.
 */
public record BookingInitiationResult(
        String bookingReferenceId,
        int httpStatus,
        String responseBody,
        boolean replayed
) {
}
