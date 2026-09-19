package com.forvmom.data.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.Instant;

/**
 * Stores one stable booking event until Kafka accepts it.
 *
 * <p>State flow: PENDING -> PROCESSING -> PUBLISHED. Failures move to FAILED
 * and Quartz retries them. After the retry limit, the row becomes COMPENSATED
 * or DEAD.
 */
@Entity
@Table(name = "booking_outbox", indexes = {
        @Index(name = "idx_outbox_status_created", columnList = "status, created_at"),
        @Index(name = "idx_outbox_booking_ref", columnList = "booking_reference_id", unique = true)
})
public class BookingOutbox {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_COMPENSATED = "COMPENSATED";
    public static final String STATUS_DEAD = "DEAD";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * e.g. MFB-1735000000000-A3F2 — client-visible reference and idempotency key
     */
    @Column(name = "booking_reference_id", nullable = false, unique = true, length = 60)
    private String bookingReferenceId;

    @Column(name = "event_id", nullable = false, unique = true, length = 60)
    private String eventId;

    @Column(name = "event_producer", nullable = false, length = 60)
    private String eventProducer;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Column(name = "causation_id", length = 100)
    private String causationId;

    /**
     * Minimal JSON:
     * {@code {userId, slotMapperId, guestCount, addonMapperIds[], pincode}}
     * Full enrichment happens asynchronously.
     */
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    /** Current lifecycle state controlled by conditional DAO transitions. */
    @Column(name = "status", nullable = false, length = 20)
    private String status = STATUS_PENDING;

    /** Failed processing attempts. At five, maintenance compensates the booking. */
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    /**
     * Identifies the current worker. Set in PROCESSING and cleared on every exit.
     * An older worker cannot update the row after a new token is assigned.
     */
    @Column(name = "processing_owner_token", length = 36)
    private String processingOwnerToken;

    @Column(name = "compensated_at")
    private LocalDateTime compensatedAt;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    public BookingOutbox() {
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBookingReferenceId() {
        return bookingReferenceId;
    }

    public void setBookingReferenceId(String bookingReferenceId) {
        this.bookingReferenceId = bookingReferenceId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventProducer() {
        return eventProducer;
    }

    public void setEventProducer(String eventProducer) {
        this.eventProducer = eventProducer;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getCausationId() {
        return causationId;
    }

    public void setCausationId(String causationId) {
        this.causationId = causationId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getProcessingStartedAt() {
        return processingStartedAt;
    }

    public void setProcessingStartedAt(LocalDateTime processingStartedAt) {
        this.processingStartedAt = processingStartedAt;
    }

    public String getProcessingOwnerToken() {
        return processingOwnerToken;
    }

    public void setProcessingOwnerToken(String processingOwnerToken) {
        this.processingOwnerToken = processingOwnerToken;
    }

    public LocalDateTime getCompensatedAt() {
        return compensatedAt;
    }

    public void setCompensatedAt(LocalDateTime compensatedAt) {
        this.compensatedAt = compensatedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
