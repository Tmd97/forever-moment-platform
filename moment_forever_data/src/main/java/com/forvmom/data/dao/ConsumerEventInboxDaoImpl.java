package com.forvmom.data.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class ConsumerEventInboxDaoImpl implements ConsumerEventInboxDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public int claim(
            String producer,
            String eventId,
            String eventType,
            String aggregateId,
            String correlationId
    ) {
        return entityManager.createNativeQuery(
                        "INSERT INTO consumer_event_inbox "
                                + "(producer, event_id, event_type, aggregate_id, correlation_id, status, received_at) "
                                + "VALUES (:producer, :eventId, :eventType, :aggregateId, :correlationId, "
                                + "'RECEIVED', CURRENT_TIMESTAMP) "
                                + "ON CONFLICT (producer, event_id) DO NOTHING")
                .setParameter("producer", producer)
                .setParameter("eventId", eventId)
                .setParameter("eventType", eventType)
                .setParameter("aggregateId", aggregateId)
                .setParameter("correlationId", correlationId)
                .executeUpdate();
    }

    @Override
    public int markProcessed(String producer, String eventId, LocalDateTime processedAt) {
        return entityManager.createQuery(
                        "UPDATE ConsumerEventInbox inbox "
                                + "SET inbox.status = :processed, inbox.processedAt = :processedAt "
                                + "WHERE inbox.producer = :producer "
                                + "AND inbox.eventId = :eventId "
                                + "AND inbox.status = :received")
                .setParameter("processed", "PROCESSED")
                .setParameter("processedAt", processedAt)
                .setParameter("producer", producer)
                .setParameter("eventId", eventId)
                .setParameter("received", "RECEIVED")
                .executeUpdate();
    }
}
