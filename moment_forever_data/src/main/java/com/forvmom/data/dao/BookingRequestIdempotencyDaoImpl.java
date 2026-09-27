package com.forvmom.data.dao;

import com.forvmom.data.entities.BookingRequestIdempotency;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class BookingRequestIdempotencyDaoImpl implements BookingRequestIdempotencyDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public int claim(
            String operation,
            String callerId,
            String idempotencyKeyHash,
            String requestFingerprint,
            String ownerToken,
            LocalDateTime leaseExpiresAt
    ) {
        return entityManager.createNativeQuery(
                        "INSERT INTO booking_request_idempotency "
                                + "(operation, caller_id, idempotency_key_hash, request_fingerprint, status, "
                                + "owner_token, lease_expires_at, created_at, updated_at) "
                                + "VALUES (:operation, :callerId, :idempotencyKey, :fingerprint, "
                                + "'IN_PROGRESS', :ownerToken, :leaseExpiresAt, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
                                + "ON CONFLICT (operation, caller_id, idempotency_key_hash) DO NOTHING")
                .setParameter("operation", operation)
                .setParameter("callerId", callerId)
                .setParameter("idempotencyKey", idempotencyKeyHash)
                .setParameter("fingerprint", requestFingerprint)
                .setParameter("ownerToken", ownerToken)
                .setParameter("leaseExpiresAt", leaseExpiresAt)
                .executeUpdate();
    }

    @Override
    public BookingRequestIdempotency find(
            String operation,
            String callerId,
            String idempotencyKeyHash
    ) {
        return query(operation, callerId, idempotencyKeyHash, false);
    }

    @Override
    public BookingRequestIdempotency findForUpdate(
            String operation,
            String callerId,
            String idempotencyKeyHash
    ) {
        return query(operation, callerId, idempotencyKeyHash, true);
    }

    private BookingRequestIdempotency query(
            String operation,
            String callerId,
            String idempotencyKeyHash,
            boolean forUpdate
    ) {
        var query = entityManager.createQuery(
                        "SELECT request FROM BookingRequestIdempotency request "
                                + "WHERE request.operation = :operation "
                                + "AND request.callerId = :callerId "
                                + "AND request.idempotencyKeyHash = :idempotencyKey",
                        BookingRequestIdempotency.class)
                .setParameter("operation", operation)
                .setParameter("callerId", callerId)
                .setParameter("idempotencyKey", idempotencyKeyHash);
        if (forUpdate) {
            query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        }
        List<BookingRequestIdempotency> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public int reclaim(
            String operation,
            String callerId,
            String idempotencyKeyHash,
            String requestFingerprint,
            String ownerToken,
            LocalDateTime leaseExpiresAt,
            LocalDateTime now
    ) {
        int updated = entityManager.createQuery(
                        "UPDATE BookingRequestIdempotency request "
                                + "SET request.status = :inProgress, request.ownerToken = :ownerToken, "
                                + "request.leaseExpiresAt = :leaseExpiresAt, request.failureReason = null, "
                                + "request.updatedAt = :now "
                                + "WHERE request.operation = :operation "
                                + "AND request.callerId = :callerId "
                                + "AND request.idempotencyKeyHash = :idempotencyKey "
                                + "AND request.requestFingerprint = :fingerprint "
                                + "AND (request.status = :failed "
                                + "OR (request.status = :inProgress AND request.leaseExpiresAt <= :now))")
                .setParameter("inProgress", BookingRequestIdempotency.STATUS_IN_PROGRESS)
                .setParameter("failed", BookingRequestIdempotency.STATUS_FAILED)
                .setParameter("ownerToken", ownerToken)
                .setParameter("leaseExpiresAt", leaseExpiresAt)
                .setParameter("now", now)
                .setParameter("operation", operation)
                .setParameter("callerId", callerId)
                .setParameter("idempotencyKey", idempotencyKeyHash)
                .setParameter("fingerprint", requestFingerprint)
                .executeUpdate();
        // Bulk DML bypasses the persistence context; clear it so a losing reclaimer
        // observes the winner's committed COMPLETED response rather than stale state.
        entityManager.clear();
        return updated;
    }

    @Override
    public int markFailed(
            String operation,
            String callerId,
            String idempotencyKeyHash,
            String ownerToken,
            String failureReason,
            LocalDateTime failedAt
    ) {
        return entityManager.createQuery(
                        "UPDATE BookingRequestIdempotency request "
                                + "SET request.status = :failed, request.failureReason = :failureReason, "
                                + "request.updatedAt = :failedAt, request.leaseExpiresAt = :failedAt "
                                + "WHERE request.operation = :operation "
                                + "AND request.callerId = :callerId "
                                + "AND request.idempotencyKeyHash = :idempotencyKey "
                                + "AND request.ownerToken = :ownerToken "
                                + "AND request.status = :inProgress")
                .setParameter("failed", BookingRequestIdempotency.STATUS_FAILED)
                .setParameter("inProgress", BookingRequestIdempotency.STATUS_IN_PROGRESS)
                .setParameter("failureReason", failureReason)
                .setParameter("failedAt", failedAt)
                .setParameter("operation", operation)
                .setParameter("callerId", callerId)
                .setParameter("idempotencyKey", idempotencyKeyHash)
                .setParameter("ownerToken", ownerToken)
                .executeUpdate();
    }

    @Override
    public int deleteCompletedOlderThan(LocalDateTime cutoff) {
        return entityManager.createQuery(
                        "DELETE FROM BookingRequestIdempotency request "
                                + "WHERE request.status = :completed "
                                + "AND request.completedAt < :cutoff")
                .setParameter("completed", BookingRequestIdempotency.STATUS_COMPLETED)
                .setParameter("cutoff", cutoff)
                .executeUpdate();
    }
}
