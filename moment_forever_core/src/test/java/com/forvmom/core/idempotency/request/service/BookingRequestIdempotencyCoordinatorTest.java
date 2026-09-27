package com.forvmom.core.idempotency.request.service;

import com.forvmom.common.errorhandler.IdempotencyConflictException;
import com.forvmom.common.errorhandler.IdempotencyRequestInProgressException;
import com.forvmom.core.idempotency.request.model.BookingRequestIdempotencyClaimResult;
import com.forvmom.data.dao.BookingRequestIdempotencyDao;
import com.forvmom.data.entities.BookingRequestIdempotency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingRequestIdempotencyCoordinatorTest {

    private static final String KEY_HASH =
            "be2974546978e3739e6d6da85c4be9f334ce32df2b9fd4b6ff1b55c0d57e9d44";

    @Mock
    private BookingRequestIdempotencyDao dao;

    @Test
    void firstRequestOwnsTheKey() {
        when(dao.claim(
                eq("CREATE_BOOKING"), eq("7"), eq(KEY_HASH), eq("fingerprint"),
                any(String.class), any(LocalDateTime.class))).thenReturn(1);
        BookingRequestIdempotencyCoordinator coordinator =
                new BookingRequestIdempotencyCoordinator(dao);

        BookingRequestIdempotencyClaimResult result =
                coordinator.claimOrReplay("CREATE_BOOKING", 7L, " key-1 ", "fingerprint");

        assertNotNull(result.claim());
        assertEquals(64, result.claim().idempotencyKeyHash().length());
    }

    @Test
    void completedRequestReplaysStableResponse() {
        BookingRequestIdempotency existing =
                existing("fingerprint", BookingRequestIdempotency.STATUS_COMPLETED);
        existing.setBookingReferenceId("MFB-100");
        existing.setHttpStatus(202);
        existing.setResponseBody("{\"stable\":true}");
        when(dao.find("CREATE_BOOKING", "7", KEY_HASH)).thenReturn(existing);
        BookingRequestIdempotencyCoordinator coordinator =
                new BookingRequestIdempotencyCoordinator(dao);

        BookingRequestIdempotencyClaimResult result =
                coordinator.claimOrReplay("CREATE_BOOKING", 7L, "key-1", "fingerprint");

        assertEquals("MFB-100", result.replay().bookingReferenceId());
        assertEquals(202, result.replay().httpStatus());
        assertEquals("{\"stable\":true}", result.replay().responseBody());
        assertEquals(true, result.replay().replayed());
    }

    @Test
    void sameKeyWithDifferentRequestConflicts() {
        when(dao.find("CREATE_BOOKING", "7", KEY_HASH)).thenReturn(
                existing("other-fingerprint", BookingRequestIdempotency.STATUS_COMPLETED));
        BookingRequestIdempotencyCoordinator coordinator =
                new BookingRequestIdempotencyCoordinator(dao);

        assertThrows(
                IdempotencyConflictException.class,
                () -> coordinator.claimOrReplay(
                        "CREATE_BOOKING", 7L, "key-1", "fingerprint"));
    }

    @Test
    void failedAttemptCanBeReclaimedBySameRequest() {
        when(dao.find("CREATE_BOOKING", "7", KEY_HASH)).thenReturn(
                existing("fingerprint", BookingRequestIdempotency.STATUS_FAILED));
        when(dao.reclaim(
                eq("CREATE_BOOKING"), eq("7"), eq(KEY_HASH), eq("fingerprint"),
                any(String.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1);
        BookingRequestIdempotencyCoordinator coordinator =
                new BookingRequestIdempotencyCoordinator(dao);

        assertNotNull(coordinator.claimOrReplay(
                "CREATE_BOOKING", 7L, "key-1", "fingerprint").claim());
    }

    @Test
    void activeOwnerReturnsDeterministicInProgressConflict() {
        BookingRequestIdempotency existing =
                existing("fingerprint", BookingRequestIdempotency.STATUS_IN_PROGRESS);
        existing.setLeaseExpiresAt(LocalDateTime.now().plusMinutes(1));
        when(dao.find("CREATE_BOOKING", "7", KEY_HASH)).thenReturn(existing);
        BookingRequestIdempotencyCoordinator coordinator =
                new BookingRequestIdempotencyCoordinator(dao);

        assertThrows(
                IdempotencyRequestInProgressException.class,
                () -> coordinator.claimOrReplay(
                        "CREATE_BOOKING", 7L, "key-1", "fingerprint"));
    }

    @Test
    void rejectsBlankAndOversizedKeys() {
        BookingRequestIdempotencyCoordinator coordinator =
                new BookingRequestIdempotencyCoordinator(dao);

        assertThrows(
                IllegalArgumentException.class,
                () -> coordinator.claimOrReplay(
                        "CREATE_BOOKING", 7L, " ", "fingerprint"));
        assertThrows(
                IllegalArgumentException.class,
                () -> coordinator.claimOrReplay(
                        "CREATE_BOOKING",
                        7L,
                        "x".repeat(BookingRequestIdempotencyCoordinator.MAX_KEY_LENGTH + 1),
                        "fingerprint"));
    }

    @Test
    void concurrentSameKeyElectsExactlyOneOwner() throws Exception {
        BookingRequestIdempotencyCoordinator coordinator =
                new BookingRequestIdempotencyCoordinator(new InMemoryDao());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<String> request = () -> {
                try {
                    BookingRequestIdempotencyClaimResult result =
                            coordinator.claimOrReplay(
                                    "CREATE_BOOKING", 7L, "key-1", "fingerprint");
                    return result.claim() == null ? "REPLAY" : "OWNER";
                } catch (IdempotencyRequestInProgressException exception) {
                    return "IN_PROGRESS";
                }
            };

            List<String> outcomes = executor.invokeAll(List.of(request, request)).stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception exception) {
                            throw new AssertionError(exception);
                        }
                    })
                    .toList();

            assertEquals(1, outcomes.stream().filter("OWNER"::equals).count());
            assertEquals(1, outcomes.stream().filter("IN_PROGRESS"::equals).count());
        } finally {
            executor.shutdownNow();
        }
    }

    private BookingRequestIdempotency existing(String fingerprint, String status) {
        BookingRequestIdempotency existing = new BookingRequestIdempotency();
        existing.setRequestFingerprint(fingerprint);
        existing.setStatus(status);
        existing.setLeaseExpiresAt(LocalDateTime.now().plusMinutes(1));
        return existing;
    }

    private static class InMemoryDao implements BookingRequestIdempotencyDao {
        private BookingRequestIdempotency record;

        @Override
        public synchronized int claim(
                String operation,
                String callerId,
                String idempotencyKey,
                String requestFingerprint,
                String ownerToken,
                LocalDateTime leaseExpiresAt
        ) {
            if (record != null) {
                return 0;
            }
            record = new BookingRequestIdempotency();
            record.setOperation(operation);
            record.setCallerId(callerId);
            record.setIdempotencyKeyHash(idempotencyKey);
            record.setRequestFingerprint(requestFingerprint);
            record.setOwnerToken(ownerToken);
            record.setStatus(BookingRequestIdempotency.STATUS_IN_PROGRESS);
            record.setLeaseExpiresAt(leaseExpiresAt);
            return 1;
        }

        @Override
        public synchronized BookingRequestIdempotency find(
                String operation,
                String callerId,
                String idempotencyKey
        ) {
            return record;
        }

        @Override
        public BookingRequestIdempotency findForUpdate(
                String operation,
                String callerId,
                String idempotencyKey
        ) {
            return record;
        }

        @Override
        public int reclaim(
                String operation,
                String callerId,
                String idempotencyKey,
                String requestFingerprint,
                String ownerToken,
                LocalDateTime leaseExpiresAt,
                LocalDateTime now
        ) {
            return 0;
        }

        @Override
        public int markFailed(
                String operation,
                String callerId,
                String idempotencyKey,
                String ownerToken,
                String failureReason,
                LocalDateTime failedAt
        ) {
            return 0;
        }

        @Override
        public int deleteCompletedOlderThan(LocalDateTime cutoff) {
            return 0;
        }
    }
}
