package com.forvmom.core.services;

import com.forvmom.common.dto.request.BookingRequestDto;
import com.forvmom.core.event_enrichment.BookingEnrichmentTask;
import com.forvmom.core.idempotency.request.model.BookingInitiationResult;
import com.forvmom.core.idempotency.request.model.BookingRequestIdempotencyClaim;
import com.forvmom.core.idempotency.request.model.BookingRequestIdempotencyClaimResult;
import com.forvmom.core.idempotency.request.service.BookingRequestFingerprintService;
import com.forvmom.core.idempotency.request.service.BookingRequestIdempotencyCoordinator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingOrchestrationServiceTest {

    @Mock
    private BookingEnrichmentTask enrichmentTask;
    @Mock
    private BookingCreationRetryService bookingCreationRetryService;
    @Mock
    private BookingRequestFingerprintService fingerprintService;
    @Mock
    private BookingRequestIdempotencyCoordinator coordinator;

    private BookingOrchestrationService service;
    private BookingRequestDto request;
    private BookingRequestIdempotencyClaim claim;

    @BeforeEach
    void setUp() {
        service = new BookingOrchestrationService(
                enrichmentTask, bookingCreationRetryService, fingerprintService, coordinator);
        request = new BookingRequestDto();
        claim = new BookingRequestIdempotencyClaim(
                "CREATE_BOOKING", "7", "key-1", "fingerprint", "owner-1");
        when(fingerprintService.createFingerprint(request)).thenReturn("fingerprint");
    }

    @Test
    void firstRequestCreatesOneBookingAndStartsEnrichment() {
        BookingInitiationResult accepted =
                new BookingInitiationResult("MFB-100", 202, "{\"bookingId\":\"MFB-100\"}", false);
        when(coordinator.claimOrReplay("CREATE_BOOKING", 7L, "key-1", "fingerprint"))
                .thenReturn(new BookingRequestIdempotencyClaimResult(claim, null));
        when(bookingCreationRetryService.reserveCapacityAndCreateOutbox(claim, request, 7L))
                .thenReturn(accepted);

        BookingInitiationResult result = service.initiateBooking(request, 7L, "key-1");

        assertEquals(accepted, result);
        verify(bookingCreationRetryService).reserveCapacityAndCreateOutbox(claim, request, 7L);
        verify(enrichmentTask).enrich("MFB-100");
    }

    @Test
    void replayReturnsStableResponseWithoutReservationOutboxOrEvent() {
        BookingInitiationResult replay =
                new BookingInitiationResult("MFB-100", 202, "{\"bookingId\":\"MFB-100\"}", true);
        when(coordinator.claimOrReplay("CREATE_BOOKING", 7L, "key-1", "fingerprint"))
                .thenReturn(new BookingRequestIdempotencyClaimResult(null, replay));

        BookingInitiationResult result = service.initiateBooking(request, 7L, "key-1");

        assertEquals(replay, result);
        verify(bookingCreationRetryService, never())
                .reserveCapacityAndCreateOutbox(claim, request, 7L);
        verify(enrichmentTask, never()).enrich("MFB-100");
    }

    @Test
    void failedAttemptIsMarkedFailedAndNextAttemptCanSucceed() {
        RuntimeException capacityFailure = new IllegalStateException("capacity unavailable");
        BookingRequestIdempotencyClaim retryClaim = new BookingRequestIdempotencyClaim(
                "CREATE_BOOKING", "7", "key-1", "fingerprint", "owner-2");
        BookingInitiationResult accepted =
                new BookingInitiationResult("MFB-101", 202, "{}", false);
        when(coordinator.claimOrReplay("CREATE_BOOKING", 7L, "key-1", "fingerprint"))
                .thenReturn(
                        new BookingRequestIdempotencyClaimResult(claim, null),
                        new BookingRequestIdempotencyClaimResult(retryClaim, null));
        when(bookingCreationRetryService.reserveCapacityAndCreateOutbox(claim, request, 7L))
                .thenThrow(capacityFailure);
        when(bookingCreationRetryService.reserveCapacityAndCreateOutbox(
                retryClaim, request, 7L))
                .thenReturn(accepted);

        assertThrows(
                IllegalStateException.class,
                () -> service.initiateBooking(request, 7L, "key-1"));
        BookingInitiationResult retry = service.initiateBooking(request, 7L, "key-1");

        assertEquals("MFB-101", retry.bookingReferenceId());
        verify(coordinator).markFailed(claim, capacityFailure);
        verify(enrichmentTask).enrich("MFB-101");
        verify(bookingCreationRetryService, times(1))
                .reserveCapacityAndCreateOutbox(retryClaim, request, 7L);
    }
}
