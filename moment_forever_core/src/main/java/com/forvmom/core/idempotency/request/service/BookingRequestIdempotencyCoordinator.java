package com.forvmom.core.idempotency.request.service;

import com.forvmom.common.errorhandler.IdempotencyConflictException;
import com.forvmom.common.errorhandler.IdempotencyRequestInProgressException;
import com.forvmom.core.idempotency.request.model.BookingInitiationResult;
import com.forvmom.core.idempotency.request.model.BookingRequestIdempotencyClaim;
import com.forvmom.core.idempotency.request.model.BookingRequestIdempotencyClaimResult;
import com.forvmom.data.dao.BookingRequestIdempotencyDao;
import com.forvmom.data.entities.BookingRequestIdempotency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Decides whether a booking request may run or must return an earlier result.
 *
 * <p>Flow: hash key -> claim it -> run once. A completed matching request is
 * replayed. A different payload conflicts. An active request returns in-progress.
 */
@Service
public class BookingRequestIdempotencyCoordinator {

    private static final Logger logger =
            LoggerFactory.getLogger(BookingRequestIdempotencyCoordinator.class);

    static final int MAX_KEY_LENGTH = 200;
    /** After this time, a crashed request owner may be replaced. */
    private static final int LEASE_MINUTES = 5;

    private final BookingRequestIdempotencyDao bookingRequestIdempotencyDao;

    public BookingRequestIdempotencyCoordinator(
            BookingRequestIdempotencyDao bookingRequestIdempotencyDao
    ) {
        this.bookingRequestIdempotencyDao = bookingRequestIdempotencyDao;
    }

    /**
     * Returns either permission to run the request or its saved response.
     *
     * @throws IdempotencyConflictException when the key belongs to another payload
     * @throws IdempotencyRequestInProgressException while another lease is active
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BookingRequestIdempotencyClaimResult claimOrReplay(
            String operation,
            Long callerId,
            String rawIdempotencyKey,
            String requestFingerprint
    ) {
        // Store only a hash of the client key; never store the raw header value.
        String idempotencyKeyHash = hashKey(validateAndNormalizeKey(rawIdempotencyKey));
        String callerScope = callerId.toString();
        String ownerToken = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime leaseExpiresAt = now.plusMinutes(LEASE_MINUTES);

        // The unique database key lets only one concurrent request insert the claim.
        int claimCount = bookingRequestIdempotencyDao.claim(
                operation,
                callerScope,
                idempotencyKeyHash,
                requestFingerprint,
                ownerToken,
                leaseExpiresAt);
        if (claimCount == 1) {
            logger.debug(
                    "Claimed new booking idempotency request: operation={}, callerId={}",
                    operation,
                    callerScope);
            return BookingRequestIdempotencyClaimResult.claimed(new BookingRequestIdempotencyClaim(
                    operation,
                    callerScope,
                    idempotencyKeyHash,
                    requestFingerprint,
                    ownerToken));
        }

        // The key already exists, so decide between conflict, replay, or waiting.
        BookingRequestIdempotency existingRequest =
                bookingRequestIdempotencyDao.find(
                        operation, callerScope, idempotencyKeyHash);
        if (existingRequest == null) {
            throw new IllegalStateException("Idempotency claim disappeared after conflict");
        }
        if (!requestFingerprint.equals(existingRequest.getRequestFingerprint())) {
            logger.warn(
                    "Rejected reused booking idempotency key with a different request: operation={}, callerId={}",
                    operation,
                    callerScope);
            throw new IdempotencyConflictException(
                    "Idempotency-Key was already used for a different booking request");
        }
        if (BookingRequestIdempotency.STATUS_COMPLETED.equals(existingRequest.getStatus())) {
            logger.info(
                    "Replaying completed booking idempotency response: operation={}, callerId={}, bookingReferenceId={}",
                    operation,
                    callerScope,
                    existingRequest.getBookingReferenceId());
            return BookingRequestIdempotencyClaimResult.replay(new BookingInitiationResult(
                    existingRequest.getBookingReferenceId(),
                    existingRequest.getHttpStatus(),
                    existingRequest.getResponseBody(),
                    true));
        }

        boolean reclaimable =
                BookingRequestIdempotency.STATUS_FAILED.equals(existingRequest.getStatus())
                        || (BookingRequestIdempotency.STATUS_IN_PROGRESS.equals(
                                existingRequest.getStatus())
                        && !existingRequest.getLeaseExpiresAt().isAfter(now));
        if (reclaimable) {
            // A failed request or expired owner may be replaced by one new owner.
            int reclaimCount = bookingRequestIdempotencyDao.reclaim(
                    operation,
                    callerScope,
                    idempotencyKeyHash,
                    requestFingerprint,
                    ownerToken,
                    leaseExpiresAt,
                    now);
            if (reclaimCount == 1) {
                logger.info(
                        "Reclaimed booking idempotency request after failure or lease expiry: operation={}, callerId={}",
                        operation,
                        callerScope);
                return BookingRequestIdempotencyClaimResult.claimed(
                        new BookingRequestIdempotencyClaim(
                                operation,
                                callerScope,
                                idempotencyKeyHash,
                                requestFingerprint,
                                ownerToken));
            }
            BookingRequestIdempotency refreshedRequest =
                    bookingRequestIdempotencyDao.find(
                            operation, callerScope, idempotencyKeyHash);
            if (refreshedRequest != null
                    && BookingRequestIdempotency.STATUS_COMPLETED.equals(
                            refreshedRequest.getStatus())) {
                logger.info(
                        "Replaying booking idempotency response completed during reclaim race: operation={}, callerId={}, bookingReferenceId={}",
                        operation,
                        callerScope,
                        refreshedRequest.getBookingReferenceId());
                return BookingRequestIdempotencyClaimResult.replay(new BookingInitiationResult(
                        refreshedRequest.getBookingReferenceId(),
                        refreshedRequest.getHttpStatus(),
                        refreshedRequest.getResponseBody(),
                        true));
            }
        }

        logger.debug(
                "Booking idempotency request remains in progress: operation={}, callerId={}",
                operation,
                callerScope);
        throw new IdempotencyRequestInProgressException(
                "A booking request with this Idempotency-Key is still in progress");
    }

    /**
     * Releases a failed claim so the same request can try again immediately.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(
            BookingRequestIdempotencyClaim claim,
            RuntimeException failure
    ) {
        int updatedCount = bookingRequestIdempotencyDao.markFailed(
                claim.operation(),
                claim.callerId(),
                claim.idempotencyKeyHash(),
                claim.ownerToken(),
                failure.getClass().getSimpleName(),
                LocalDateTime.now());
        if (updatedCount != 1) {
            throw new IllegalStateException(
                    "Could not mark owned idempotency request as failed");
        }
        logger.warn(
                "Marked booking idempotency request failed: operation={}, callerId={}, failureType={}",
                claim.operation(),
                claim.callerId(),
                failure.getClass().getSimpleName());
    }

    /**
     * Rejects a missing or oversized key.
     */
    private String validateAndNormalizeKey(String rawIdempotencyKey) {
        if (rawIdempotencyKey == null || rawIdempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key must not be blank");
        }
        String idempotencyKey = rawIdempotencyKey.trim();
        if (idempotencyKey.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "Idempotency-Key must not exceed " + MAX_KEY_LENGTH + " characters");
        }
        return idempotencyKey;
    }

    /**
     * Hashes the key before it reaches the database.
     */
    private String hashKey(String idempotencyKey) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(idempotencyKey.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
