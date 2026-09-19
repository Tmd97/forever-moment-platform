package com.forvmom.data.dao;

import java.time.LocalDateTime;

public interface ConsumerEventInboxDao {

    /**
     * Inserts a received delivery unless its producer/event identity already exists.
     *
     * @return one for the first delivery, otherwise zero
     */
    int claim(
            String producer,
            String eventId,
            String eventType,
            String aggregateId,
            String correlationId);

    /**
     * Marks a claimed delivery processed after its business effect succeeds.
     */
    int markProcessed(String producer, String eventId, LocalDateTime processedAt);
}
