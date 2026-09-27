package com.forvmom.core.retries.model;

/**
 * Distinguishes a stale compensation candidate from an applied compensation
 * whose reservation may already have been released by another business path.
 */
public record BookingOutboxCompensationResult(
        boolean applied,
        boolean released
) {
    /**
     * Creates a result for a row that failed locked eligibility revalidation.
     */
    public static BookingOutboxCompensationResult skipped() {
        return new BookingOutboxCompensationResult(false, false);
    }

    /**
     * Creates a result for an applied terminal transition.
     */
    public static BookingOutboxCompensationResult applied(boolean released) {
        return new BookingOutboxCompensationResult(true, released);
    }
}
