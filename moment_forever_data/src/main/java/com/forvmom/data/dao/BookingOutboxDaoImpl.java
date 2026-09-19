package com.forvmom.data.dao;

import com.forvmom.data.entities.BookingOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@Transactional
public class BookingOutboxDaoImpl extends GenericDaoImpl<BookingOutbox, Long>
        implements BookingOutboxDao {

    public BookingOutboxDaoImpl() {
        super(BookingOutbox.class);
    }

    @Override
    public BookingOutbox findByBookingReferenceId(String bookingReferenceId) {
        return findByBookingReferenceId(bookingReferenceId, false);
    }

    @Override
    public BookingOutbox findForUpdate(String bookingReferenceId) {
        return findByBookingReferenceId(bookingReferenceId, true);
    }

    private BookingOutbox findByBookingReferenceId(
            String bookingReferenceId,
            boolean forUpdate
    ) {
        var query = em.createQuery(
                        "SELECT o FROM BookingOutbox o WHERE o.bookingReferenceId = :ref",
                        BookingOutbox.class)
                .setParameter("ref", bookingReferenceId);
        if (forUpdate) {
            query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        }
        List<BookingOutbox> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<BookingOutbox> findRetryable(LocalDateTime olderThan, int maxRetries) {
        return em.createQuery(
                        "SELECT o FROM BookingOutbox o " +
                                "WHERE o.status IN ('PENDING', 'FAILED') " +
                                "  AND o.createdAt < :olderThan " +
                                "  AND o.retryCount < :maxRetries " +
                                "ORDER BY o.createdAt ASC",
                        BookingOutbox.class)
                .setParameter("olderThan", olderThan)
                .setParameter("maxRetries", maxRetries)
                .setMaxResults(20)
                .getResultList();
    }

    @Override
    public List<BookingOutbox> findUnresolved(LocalDateTime olderThan) {
        return em.createQuery(
                        "SELECT o FROM BookingOutbox o " +
                                "WHERE o.status IN ('PENDING', 'FAILED') " +
                                "  AND o.createdAt < :olderThan " +
                                "ORDER BY o.createdAt ASC",
                        BookingOutbox.class)
                .setParameter("olderThan", olderThan)
                .setMaxResults(20)
                .getResultList();
    }


    @Override
    public int deletePublishedOlderThan(LocalDateTime cutoff) {
        return em.createQuery(
                        "DELETE FROM BookingOutbox o " +
                                "WHERE o.status = :published AND o.publishedAt < :cutoff")
                .setParameter("published", BookingOutbox.STATUS_PUBLISHED)
                .setParameter("cutoff", cutoff)
                .executeUpdate();
    }

    @Override
    public int markAsProcessing(
            String bookingReferenceId,
            String ownerToken,
            LocalDateTime processingStartedAt
    ) {
        // Only one worker can change PENDING/FAILED to PROCESSING and store its token.
        return em.createQuery(
                        "UPDATE BookingOutbox o " +
                                "SET o.status = :processing, " +
                                "    o.processingStartedAt = :startedAt, " +
                                "    o.processingOwnerToken = :ownerToken, " +
                                "    o.failureReason = null " +
                                "WHERE o.bookingReferenceId = :ref AND o.status IN (:pending, :failed)")
                .setParameter("processing", BookingOutbox.STATUS_PROCESSING)
                .setParameter("startedAt", processingStartedAt)
                .setParameter("ownerToken", ownerToken)
                .setParameter("ref", bookingReferenceId)
                .setParameter("pending", BookingOutbox.STATUS_PENDING)
                .setParameter("failed", BookingOutbox.STATUS_FAILED)
                .executeUpdate();
    }

    @Override
    public int markPublished(
            String bookingReferenceId,
            String ownerToken,
            LocalDateTime publishedAt
    ) {
        // The token check blocks a timed-out worker after another worker takes over.
        return em.createQuery(
                        "UPDATE BookingOutbox o " +
                                "SET o.status = :published, " +
                                "    o.publishedAt = :publishedAt, " +
                                "    o.processingStartedAt = null, " +
                                "    o.processingOwnerToken = null " +
                                "WHERE o.bookingReferenceId = :ref " +
                                "  AND o.status = :processing " +
                                "  AND o.processingOwnerToken = :ownerToken")
                .setParameter("published", BookingOutbox.STATUS_PUBLISHED)
                .setParameter("processing", BookingOutbox.STATUS_PROCESSING)
                .setParameter("publishedAt", publishedAt)
                .setParameter("ownerToken", ownerToken)
                .setParameter("ref", bookingReferenceId)
                .executeUpdate();
    }

    @Override
    public int markFailed(
            String bookingReferenceId,
            String ownerToken,
            String failureReason
    ) {
        // Count the failure and clear ownership only for the current token.
        return em.createQuery(
                        "UPDATE BookingOutbox o " +
                                "SET o.status = :failed, " +
                                "    o.retryCount = o.retryCount + 1, " +
                                "    o.processingStartedAt = null, " +
                                "    o.processingOwnerToken = null, " +
                                "    o.failureReason = :failureReason " +
                                "WHERE o.bookingReferenceId = :ref " +
                                "  AND o.status = :processing " +
                                "  AND o.processingOwnerToken = :ownerToken")
                .setParameter("failed", BookingOutbox.STATUS_FAILED)
                .setParameter("processing", BookingOutbox.STATUS_PROCESSING)
                .setParameter("failureReason", failureReason)
                .setParameter("ownerToken", ownerToken)
                .setParameter("ref", bookingReferenceId)
                .executeUpdate();
    }

    @Override
    public int markCompensated(
            String bookingReferenceId,
            int minimumRetryCount,
            LocalDateTime compensatedAt
    ) {
        return em.createQuery(
                        "UPDATE BookingOutbox o " +
                                "SET o.status = :compensated, " +
                                "    o.compensatedAt = :compensatedAt, " +
                                "    o.processingStartedAt = null, " +
                                "    o.processingOwnerToken = null " +
                                "WHERE o.bookingReferenceId = :ref " +
                                "  AND o.status IN (:pending, :failed) " +
                                "  AND o.retryCount >= :minimumRetryCount " +
                                "  AND o.processingOwnerToken IS NULL")
                .setParameter("compensated", BookingOutbox.STATUS_COMPENSATED)
                .setParameter("pending", BookingOutbox.STATUS_PENDING)
                .setParameter("failed", BookingOutbox.STATUS_FAILED)
                .setParameter("compensatedAt", compensatedAt)
                .setParameter("minimumRetryCount", minimumRetryCount)
                .setParameter("ref", bookingReferenceId)
                .executeUpdate();
    }

    @Override
    public int markDead(
            String bookingReferenceId,
            int minimumRetryCount,
            String failureReason
    ) {
        return em.createQuery(
                        "UPDATE BookingOutbox o " +
                                "SET o.status = :dead, " +
                                "    o.processingStartedAt = null, " +
                                "    o.processingOwnerToken = null, " +
                                "    o.failureReason = :failureReason " +
                                "WHERE o.bookingReferenceId = :ref " +
                                "  AND o.status IN (:pending, :failed) " +
                                "  AND o.retryCount >= :minimumRetryCount " +
                                "  AND o.processingOwnerToken IS NULL")
                .setParameter("dead", BookingOutbox.STATUS_DEAD)
                .setParameter("pending", BookingOutbox.STATUS_PENDING)
                .setParameter("failed", BookingOutbox.STATUS_FAILED)
                .setParameter("failureReason", failureReason)
                .setParameter("minimumRetryCount", minimumRetryCount)
                .setParameter("ref", bookingReferenceId)
                .executeUpdate();
    }

    @Override
    public int resetStuckProcessing(LocalDateTime cutoff) {
        // Clearing the token fences the timed-out worker before the row is retried.
        int updated = em.createQuery(
                        "UPDATE BookingOutbox o " +
                                "SET o.status = :failed, " +
                                "    o.retryCount = o.retryCount + 1, " +
                                "    o.processingStartedAt = null, " +
                                "    o.processingOwnerToken = null " +
                                "WHERE o.status = :processing " +
                                "  AND o.processingStartedAt < :cutoff")
                .setParameter("failed", BookingOutbox.STATUS_FAILED)
                .setParameter("processing", BookingOutbox.STATUS_PROCESSING)
                .setParameter("cutoff", cutoff)
                .executeUpdate();
        em.clear();
        return updated;
    }
}
