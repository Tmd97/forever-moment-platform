package com.forvmom.core.services;

import com.forvmom.common.dto.request.BookingRequestDto;
import com.forvmom.core.event_enrichment.*;
import com.forvmom.core.idempotency.request.model.BookingInitiationResult;
import com.forvmom.core.idempotency.request.model.BookingRequestIdempotencyClaimResult;
import com.forvmom.core.idempotency.request.service.BookingRequestFingerprintService;
import com.forvmom.core.idempotency.request.service.BookingRequestIdempotencyCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Booking creation flow:
 * fingerprint request -> claim/replay key -> reserve and save outbox -> start publish.
 */
@Service
public class BookingOrchestrationService {

    private static final Logger logger = LoggerFactory.getLogger(BookingOrchestrationService.class);
    private static final String CREATE_BOOKING_OPERATION = "CREATE_BOOKING";

    private final BookingEnrichmentTask bookingEnrichmentTask;
    private final BookingCreationRetryService bookingCreationRetryService;
    private final BookingRequestFingerprintService requestFingerprintService;
    private final BookingRequestIdempotencyCoordinator idempotencyCoordinator;

    public BookingOrchestrationService(
            BookingEnrichmentTask bookingEnrichmentTask,
            BookingCreationRetryService bookingCreationRetryService,
            BookingRequestFingerprintService requestFingerprintService,
            BookingRequestIdempotencyCoordinator idempotencyCoordinator
    ) {
        this.bookingEnrichmentTask = bookingEnrichmentTask;
        this.bookingCreationRetryService = bookingCreationRetryService;
        this.requestFingerprintService = requestFingerprintService;
        this.idempotencyCoordinator = idempotencyCoordinator;
    }

    /**
     * Starts a booking once, or returns the response saved by the first request.
     */
    public BookingInitiationResult initiateBooking(
           BookingRequestDto bookingRequest,
           Long userId,
           String idempotencyKey
    ) {
        // The same logical request always produces the same fingerprint.
        String requestFingerprint =
               requestFingerprintService.createFingerprint(bookingRequest);

        // Only the claim owner may create the booking. Completed keys return a replay.
        BookingRequestIdempotencyClaimResult claimResult =
               idempotencyCoordinator.claimOrReplay(
                       CREATE_BOOKING_OPERATION,
                       userId,
                       idempotencyKey,
                       requestFingerprint);
        if (claimResult.isReplay()) {
           return claimResult.replay();
        }

        BookingInitiationResult result;
        try {
           // Reservation, outbox row, and replay response are saved together.
           result = bookingCreationRetryService.reserveCapacityAndCreateOutbox(
                   claimResult.claim(), bookingRequest, userId);
        } catch (RuntimeException failure) {
           try {
               idempotencyCoordinator.markFailed(claimResult.claim(), failure);
           } catch (RuntimeException stateFailure) {
               failure.addSuppressed(stateFailure);
               logger.error(
                       "Could not persist failed booking idempotency state",
                       stateFailure);
           }
           throw failure;
        }
        try {
           // Publishing is asynchronous; Quartz retries it if this fast path fails.
           bookingEnrichmentTask.enrich(result.bookingReferenceId());
        } catch (Exception exception) {
           logger.warn(
                   "Fast-path enrichment rejected; scheduler will retry",
                   exception);
        }
        return result;
    }
}
