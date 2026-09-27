package com.forvmom.core.idempotency.event.model;

/**
 * Outcome used by the Kafka adapter for observability and acknowledgment.
 */
public enum EventProcessingResult {
    PROCESSED,
    DUPLICATE_EVENT,
    BUSINESS_NO_OP
}
