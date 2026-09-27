package com.forvmom.core.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forvmom.common.dto.request.BookingRequestDto;
import com.forvmom.core.idempotency.request.model.BookingRequestIdempotencyClaim;
import com.forvmom.core.idempotency.request.model.BookingInitiationResult;
import com.forvmom.data.dao.BookingOutboxDao;
import com.forvmom.data.dao.BookingReservationDao;
import com.forvmom.data.dao.BookingRequestIdempotencyDao;
import com.forvmom.data.entities.BookingOutbox;
import com.forvmom.data.entities.BookingReservation;
import com.forvmom.data.entities.BookingRequestIdempotency;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.common.utils.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Atomically creates one booking result.
 *
 * <p>The inventory reservation, reservation record, outbox event, and saved
 * idempotency response either all commit or all roll back.
 */
@Service
public class BookingCreationTransactionService {

    private static final Logger logger =
            LoggerFactory.getLogger(BookingCreationTransactionService.class);
    private static final String EVENT_PRODUCER = "moment-forever-core";
    private static final int EVENT_SCHEMA_VERSION = 1;

    private final SlotInventoryService slotInventoryService;
    private final BookingOutboxDao bookingOutboxDao;
    private final BookingReservationDao bookingReservationDao;
    private final BookingRequestIdempotencyDao bookingRequestIdempotencyDao;
    private final ObjectMapper objectMapper;

    public BookingCreationTransactionService(
            SlotInventoryService slotInventoryService,
            BookingOutboxDao bookingOutboxDao,
            BookingReservationDao bookingReservationDao,
            BookingRequestIdempotencyDao bookingRequestIdempotencyDao,
            ObjectMapper objectMapper
    ) {
        this.slotInventoryService = slotInventoryService;
        this.bookingOutboxDao = bookingOutboxDao;
        this.bookingReservationDao = bookingReservationDao;
        this.bookingRequestIdempotencyDao = bookingRequestIdempotencyDao;
        this.objectMapper = objectMapper;
    }

    /**
     * Verifies request ownership, reserves capacity, creates the outbox event,
     * and stores the HTTP response for future replay.
     */
    @Transactional
    public BookingInitiationResult reserveCapacityAndCreateOutbox(
            BookingRequestIdempotencyClaim claim,
            BookingRequestDto bookingRequest,
            Long userId
    ) {
        // TO DO:since we are checking the idempotency of request in BookingOrchestrationService, we can remove this check here and also remove the claim parameter from this method.
        // Lock the idempotency row so only the current request owner can create data.
        BookingRequestIdempotency idempotencyRequest =
                bookingRequestIdempotencyDao.findForUpdate(
                claim.operation(), claim.callerId(), claim.idempotencyKeyHash());
        if (idempotencyRequest == null
                || !BookingRequestIdempotency.STATUS_IN_PROGRESS.equals(
                        idempotencyRequest.getStatus())
                || !claim.ownerToken().equals(idempotencyRequest.getOwnerToken())) {
            throw new IllegalStateException("Booking idempotency ownership was lost");
        }

        // Reserve inventory and remember enough data to release it if publishing fails.
        String bookingReferenceId = generateBookingReferenceId();
        slotInventoryService.reserveCapacity(
                bookingRequest.getTimeSlotMapperId(),
                bookingRequest.getBookingDate(),
                bookingRequest.getGuestCount());

        BookingReservation reservation = new BookingReservation();
        reservation.setBookingReferenceId(bookingReferenceId);
        reservation.setSlotMapperId(bookingRequest.getTimeSlotMapperId());
        reservation.setBookingDate(bookingRequest.getBookingDate());
        reservation.setGuestCount(bookingRequest.getGuestCount());
        reservation.setStatus(BookingReservation.STATUS_RESERVED);
        bookingReservationDao.save(reservation);

        // The event ID is created once here and reused by every outbox retry.
        BookingOutbox outbox = new BookingOutbox();
        outbox.setBookingReferenceId(bookingReferenceId);
        outbox.setEventId(UUID.randomUUID().toString());
        outbox.setEventProducer(EVENT_PRODUCER);
        outbox.setSchemaVersion(EVENT_SCHEMA_VERSION);
        outbox.setOccurredAt(Instant.now());
        outbox.setCorrelationId(bookingReferenceId);
        outbox.setPayload(buildMinimalPayload(userId, bookingRequest));
        outbox.setStatus(BookingOutbox.STATUS_PENDING);
        outbox.setRetryCount(0);
        outbox.setCreatedAt(LocalDateTime.now());
        bookingOutboxDao.save(outbox);

        // Save the exact accepted response so repeated HTTP calls return the same body.
        int httpStatus = 202;
        String responseBody = buildResponseBody(bookingReferenceId);
        LocalDateTime completedAt = LocalDateTime.now();
        idempotencyRequest.setBookingReferenceId(bookingReferenceId);
        idempotencyRequest.setHttpStatus(httpStatus);
        idempotencyRequest.setResponseBody(responseBody);
        idempotencyRequest.setStatus(BookingRequestIdempotency.STATUS_COMPLETED);
        idempotencyRequest.setCompletedAt(completedAt);
        idempotencyRequest.setUpdatedAt(completedAt);
        idempotencyRequest.setLeaseExpiresAt(completedAt);

        logger.info(
                "Initial booking outbox persisted: bookingReferenceId={}, eventId={}, correlationId={}",
                bookingReferenceId,
                outbox.getEventId(),
                outbox.getCorrelationId());
        return new BookingInitiationResult(
                bookingReferenceId, httpStatus, responseBody, false);
    }

    /**
     * Creates the client-visible booking reference.
     */
    private String generateBookingReferenceId() {
        return AppConstants.BOOKING_REFERENCE_PREFIX
                + System.currentTimeMillis()
                + "-"
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    /**
     * Builds the response stored for idempotent replay.
     */
    private String buildResponseBody(String bookingReferenceId) {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("bookingId", bookingReferenceId);
        data.put("status", "PENDING");
        data.put("message", "Your booking request has been received and is being processed.");
        try {
            return objectMapper.writeValueAsString(
                    ResponseUtil.buildOkResponse(data, "Booking request accepted"));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize booking response", exception);
        }
    }

    /**
     * Stores only the fields needed to build the outgoing booking event later.
     */
    private String buildMinimalPayload(Long userId, BookingRequestDto bookingRequest) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("slotMapperId", bookingRequest.getTimeSlotMapperId());
        payload.put("guestCount", bookingRequest.getGuestCount());
        payload.put(
                "addonMapperIds",
                bookingRequest.getAddonMapperIds() != null
                        ? bookingRequest.getAddonMapperIds()
                        : List.of());
        payload.put("pincode", bookingRequest.getPincode());
        payload.put("bookingDate", bookingRequest.getBookingDate().toString());

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize booking payload", exception);
        }
    }
}
