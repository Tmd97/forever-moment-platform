package com.forvmom.core.idempotency.event.model;

/**
 * Compound identity used to deduplicate deliveries independently per producer.
 */
public record ProducerEventIdentity(String producer, String eventId) {
}
