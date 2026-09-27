package com.forvmom.core.retries.model;

import com.forvmom.data.entities.BookingOutbox;

import java.util.List;

/**
 * Database snapshot produced before recovery dispatch and compensation begin.
 */
public record BookingOutboxRecoveryBatch(
        int resetCount,
        List<BookingOutbox> unresolvedRecords
) {
}
