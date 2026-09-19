package com.forvmom.core.services;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCreationTransactionServiceTest {

    @Mock
    private SlotInventoryService slotInventoryService;
    @Mock
    private BookingOutboxDao bookingOutboxDao;
    @Mock
    private BookingReservationDao bookingReservationDao;
    @Mock
    private BookingRequestIdempotencyDao idempotencyDao;

    private ObjectMapper objectMapper;
    private BookingCreationTransactionService bookingCreationTransactionService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        bookingCreationTransactionService = new BookingCreationTransactionService(
                slotInventoryService,
                bookingOutboxDao,
                bookingReservationDao,
                idempotencyDao,
                objectMapper);
    }

    @Test
    void reservesCapacityAndCreatesOutboxWithEventIdentity() throws Exception {
        LocalDate bookingDate = LocalDate.of(2026, 8, 20);
        BookingRequestDto request = new BookingRequestDto();
        request.setTimeSlotMapperId(100L);
        request.setBookingDate(bookingDate);
        request.setGuestCount(4);
        request.setAddonMapperIds(List.of(3L));
        request.setPincode("560001");

        BookingRequestIdempotency idempotency = new BookingRequestIdempotency();
        idempotency.setStatus(BookingRequestIdempotency.STATUS_IN_PROGRESS);
        idempotency.setOwnerToken("owner-1");
        when(idempotencyDao.findForUpdate("CREATE_BOOKING", "7", "booking-key"))
                .thenReturn(idempotency);
        BookingRequestIdempotencyClaim claim = new BookingRequestIdempotencyClaim(
                "CREATE_BOOKING", "7", "booking-key", "fingerprint", "owner-1");

        BookingInitiationResult result =
                bookingCreationTransactionService.reserveCapacityAndCreateOutbox(
                        claim, request, 7L);

        verify(slotInventoryService).reserveCapacity(100L, bookingDate, 4);
        ArgumentCaptor<BookingReservation> reservationCaptor =
                ArgumentCaptor.forClass(BookingReservation.class);
        verify(bookingReservationDao).save(reservationCaptor.capture());
        assertEquals(BookingReservation.STATUS_RESERVED, reservationCaptor.getValue().getStatus());

        ArgumentCaptor<BookingOutbox> outboxCaptor = ArgumentCaptor.forClass(BookingOutbox.class);
        verify(bookingOutboxDao).save(outboxCaptor.capture());
        BookingOutbox outbox = outboxCaptor.getValue();

        JsonNode payload = objectMapper.readTree(outbox.getPayload());
        assertEquals("2026-08-20", payload.get("bookingDate").asText());
        assertEquals(100L, payload.get("slotMapperId").asLong());
        assertEquals(4, payload.get("guestCount").asInt());
        assertNotNull(outbox.getEventId());
        assertEquals("moment-forever-core", outbox.getEventProducer());
        assertEquals(1, outbox.getSchemaVersion());
        assertNotNull(outbox.getOccurredAt());
        assertEquals(result.bookingReferenceId(), outbox.getCorrelationId());
        assertEquals(202, result.httpStatus());
        assertEquals(BookingRequestIdempotency.STATUS_COMPLETED, idempotency.getStatus());
        assertEquals(result.bookingReferenceId(), idempotency.getBookingReferenceId());
        assertEquals(result.responseBody(), idempotency.getResponseBody());
    }
}
