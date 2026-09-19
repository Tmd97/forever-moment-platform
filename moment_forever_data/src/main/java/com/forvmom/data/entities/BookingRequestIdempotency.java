package com.forvmom.data.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * Stores ownership and the final response for one scoped HTTP idempotency key.
 *
 * <p>IN_PROGRESS owns a short lease. COMPLETED can be replayed. FAILED can be
 * claimed again by the same request fingerprint.
 */
@Entity
@Table(
        name = "booking_request_idempotency",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_booking_request_idempotency_scope",
                columnNames = {"operation", "caller_id", "idempotency_key_hash"}))
public class BookingRequestIdempotency {

    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operation", nullable = false, length = 60)
    private String operation;

    @Column(name = "caller_id", nullable = false, length = 100)
    private String callerId;

    @Column(name = "idempotency_key_hash", nullable = false, length = 64)
    private String idempotencyKeyHash;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** Identifies the request allowed to change this row while it is IN_PROGRESS. */
    @Column(name = "owner_token", nullable = false, length = 36)
    private String ownerToken;

    @Column(name = "lease_expires_at", nullable = false)
    private LocalDateTime leaseExpiresAt;

    @Column(name = "booking_reference_id", length = 60)
    private String bookingReferenceId;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getCallerId() {
        return callerId;
    }

    public void setCallerId(String callerId) {
        this.callerId = callerId;
    }

    public String getIdempotencyKeyHash() {
        return idempotencyKeyHash;
    }

    public void setIdempotencyKeyHash(String idempotencyKeyHash) {
        this.idempotencyKeyHash = idempotencyKeyHash;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public void setRequestFingerprint(String requestFingerprint) {
        this.requestFingerprint = requestFingerprint;
    }

    public String getStatus() {
        return status;
    }

    public String getOwnerToken() {
        return ownerToken;
    }

    public LocalDateTime getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public String getBookingReferenceId() {
        return bookingReferenceId;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setOwnerToken(String ownerToken) {
        this.ownerToken = ownerToken;
    }

    public void setLeaseExpiresAt(LocalDateTime leaseExpiresAt) {
        this.leaseExpiresAt = leaseExpiresAt;
    }

    public void setBookingReferenceId(String bookingReferenceId) {
        this.bookingReferenceId = bookingReferenceId;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
