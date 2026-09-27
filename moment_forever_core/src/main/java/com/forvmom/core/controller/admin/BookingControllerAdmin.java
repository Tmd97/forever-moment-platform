package com.forvmom.core.controller.admin;

import com.forvmom.common.dto.request.BookingRequestDto;
import com.forvmom.core.idempotency.request.model.BookingInitiationResult;
import com.forvmom.core.services.BookingOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin-facing booking controller mounted at {@code /admin/bookings}.
 *
 * <p>
 * Caller identity is supplied by the API gateway as {@code X-User-Id} and
 * {@code X-User-Roles} headers rather than read from the security context, which
 * lets an admin initiate a booking on behalf of another user. These headers are
 * trusted, so the gateway must strip any client-supplied values.
 *
 * <p>
 * Flow: atomic inventory increment + outbox insert (one transaction) &rarr; 202
 * Accepted &rarr; async enrichment &rarr; {@code booking-requested} Kafka event.
 *
 * @see BookingOrchestrationService
 */
@RestController
@RequestMapping("/admin/bookings")
@Tag(name = "Booking API", description = "Endpoints for creating booking requests")
public class BookingControllerAdmin {

    private final BookingOrchestrationService bookingOrchestrationService;

    public BookingControllerAdmin(BookingOrchestrationService bookingOrchestrationService) {
        this.bookingOrchestrationService = bookingOrchestrationService;
    }

    /**
     * Accepts one idempotent booking request.
     *
     * <p>The first key/request pair returns 202. Repeating the same pair returns
     * the same status and body with {@code Idempotency-Replayed: true}. Reusing
     * the key with a different request returns 409.
     *
     * @param userId            caller identity injected by the API gateway
     * @param roles             caller roles injected by the API gateway
     * @param idempotencyKey    required caller-chosen retry identity
     * @param bookingRequestDto the booking request payload
     * @return HTTP 202 with the generated booking reference ID
     */
    @PostMapping
    @Operation(summary = "Create Booking Request", description = "Reserves capacity atomically, writes an outbox record, then asynchronously "
            + "enriches and publishes the booking-requested event. Returns a booking reference ID immediately. "
            + "Requires USER authentication.")
    public ResponseEntity<String> createBooking(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Roles") String roles,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody BookingRequestDto bookingRequestDto) {

        if (idempotencyKey == null) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }
        BookingInitiationResult result =
                bookingOrchestrationService.initiateBooking(bookingRequestDto, userId, idempotencyKey);
        ResponseEntity.BodyBuilder response = ResponseEntity
                .status(result.httpStatus())
                .contentType(MediaType.APPLICATION_JSON);
        if (result.replayed()) {
            response.header("Idempotency-Replayed", "true");
        }
        return response.body(result.responseBody());
    }
}
