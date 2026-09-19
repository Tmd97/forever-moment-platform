package com.forvmom.common.dto.events;

import java.time.Instant;

public abstract class BaseEvent {
    private String eventId;
    private String producer;
    private Integer schemaVersion;
    private Instant occurredAt;
    private String correlationId;
    private String causationId;
    private String eventType;

    public BaseEvent() {
        this.eventType = this.getClass().getSimpleName();
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getProducer() { return producer; }
    public void setProducer(String producer) { this.producer = producer; }

    public Integer getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(Integer schemaVersion) { this.schemaVersion = schemaVersion; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }

    /**
     * Backward-compatible alias for older event consumers.
     */
    public Instant getTimestamp() { return occurredAt; }
    public void setTimestamp(Instant timestamp) { this.occurredAt = timestamp; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getCausationId() { return causationId; }
    public void setCausationId(String causationId) { this.causationId = causationId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
}