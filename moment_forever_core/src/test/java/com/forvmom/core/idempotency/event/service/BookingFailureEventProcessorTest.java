package com.forvmom.core.idempotency.event.service;

import com.forvmom.common.dto.events.BookingFailedEvent;
import com.forvmom.core.idempotency.event.model.EventProcessingResult;
import com.forvmom.core.services.BookingReservationService;
import com.forvmom.data.dao.ConsumerEventInboxDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingFailureEventProcessorTest {

    @Mock
    private ConsumerEventInboxDao inboxDao;
    @Mock
    private BookingReservationService reservationService;

    private BookingFailureEventProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new BookingFailureEventProcessor(inboxDao, reservationService);
    }

    @Test
    void exactDuplicateSkipsBusinessEffect() {
        BookingFailedEvent event = event("booking-service", "evt-1");
        when(inboxDao.claim(
                "booking-service", "evt-1", "BookingFailedEvent", "MFB-100", "corr-1"))
                .thenReturn(0);

        assertEquals(
                EventProcessingResult.DUPLICATE_EVENT,
                processor.process(event));

        verify(reservationService, never()).releaseOnce(
                any(), any(), any(), any());
        verify(inboxDao, never()).markProcessed(any(), any(), any());
    }

    @Test
    void sameEventIdFromDifferentProducersUsesSeparateNamespaces() {
        BookingFailedEvent first = event("booking-service", "evt-1");
        BookingFailedEvent second = event("recovery-service", "evt-1");
        when(inboxDao.claim(
                "booking-service", "evt-1", "BookingFailedEvent", "MFB-100", "corr-1"))
                .thenReturn(1);
        when(inboxDao.claim(
                "recovery-service", "evt-1", "BookingFailedEvent", "MFB-100", "corr-1"))
                .thenReturn(1);
        when(reservationService.releaseOnce(
                "MFB-100", 100L, LocalDate.of(2026, 8, 20), 4))
                .thenReturn(true, false);
        when(inboxDao.markProcessed(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("evt-1"),
                any()))
                .thenReturn(1);

        assertEquals(
                EventProcessingResult.PROCESSED,
                processor.process(first));
        assertEquals(
                EventProcessingResult.BUSINESS_NO_OP,
                processor.process(second));

        verify(inboxDao).markProcessed(
                org.mockito.ArgumentMatchers.eq("booking-service"),
                org.mockito.ArgumentMatchers.eq("evt-1"),
                any());
        verify(inboxDao).markProcessed(
                org.mockito.ArgumentMatchers.eq("recovery-service"),
                org.mockito.ArgumentMatchers.eq("evt-1"),
                any());
        verify(reservationService, times(2)).releaseOnce(
                "MFB-100", 100L, LocalDate.of(2026, 8, 20), 4);
    }

    @Test
    void distinctEventsForSameBookingAreBothRecordedButCapacityReleasesOnce() {
        BookingFailedEvent first = event("booking-service", "evt-1");
        BookingFailedEvent second = event("booking-service", "evt-2");
        when(inboxDao.claim(
                org.mockito.ArgumentMatchers.eq("booking-service"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("BookingFailedEvent"),
                org.mockito.ArgumentMatchers.eq("MFB-100"),
                org.mockito.ArgumentMatchers.eq("corr-1")))
                .thenReturn(1);
        when(reservationService.releaseOnce(
                "MFB-100", 100L, LocalDate.of(2026, 8, 20), 4))
                .thenReturn(true, false);
        when(inboxDao.markProcessed(
                org.mockito.ArgumentMatchers.eq("booking-service"),
                org.mockito.ArgumentMatchers.anyString(),
                any()))
                .thenReturn(1);

        assertEquals(
                EventProcessingResult.PROCESSED,
                processor.process(first));
        assertEquals(
                EventProcessingResult.BUSINESS_NO_OP,
                processor.process(second));

        verify(inboxDao).markProcessed(
                org.mockito.ArgumentMatchers.eq("booking-service"),
                org.mockito.ArgumentMatchers.eq("evt-1"),
                any());
        verify(inboxDao).markProcessed(
                org.mockito.ArgumentMatchers.eq("booking-service"),
                org.mockito.ArgumentMatchers.eq("evt-2"),
                any());
        verify(reservationService, times(2)).releaseOnce(
                "MFB-100", 100L, LocalDate.of(2026, 8, 20), 4);
    }

    @Test
    void fullyLegacyEventUsesReservationGuardWithoutInboxRow() {
        BookingFailedEvent legacy = event(null, null);
        when(reservationService.releaseOnce(
                "MFB-100", 100L, LocalDate.of(2026, 8, 20), 4))
                .thenReturn(false);

        assertEquals(
                EventProcessingResult.BUSINESS_NO_OP,
                processor.process(legacy));

        verify(inboxDao, never()).claim(any(), any(), any(), any(), any());
    }

    @Test
    void rejectsEitherFormOfPartialIdentityBeforeBusinessEffect() {
        assertThrows(
                IllegalArgumentException.class,
                () -> processor.process(event("booking-service", null)));
        assertThrows(
                IllegalArgumentException.class,
                () -> processor.process(event(null, "evt-1")));

        verify(inboxDao, never()).claim(any(), any(), any(), any(), any());
        verify(reservationService, never()).releaseOnce(any(), any(), any(), any());
    }

    @Test
    void atomicClaimTurnsConcurrentLoserIntoDuplicate() {
        BookingFailedEvent event = event("booking-service", "evt-concurrent");
        when(inboxDao.claim(
                "booking-service",
                "evt-concurrent",
                "BookingFailedEvent",
                "MFB-100",
                "corr-1"))
                .thenReturn(1, 0);
        when(reservationService.releaseOnce(
                "MFB-100", 100L, LocalDate.of(2026, 8, 20), 4))
                .thenReturn(true);
        when(inboxDao.markProcessed(
                org.mockito.ArgumentMatchers.eq("booking-service"),
                org.mockito.ArgumentMatchers.eq("evt-concurrent"),
                any()))
                .thenReturn(1);

        assertEquals(
                EventProcessingResult.PROCESSED,
                processor.process(event));
        assertEquals(
                EventProcessingResult.DUPLICATE_EVENT,
                processor.process(event));

        verify(reservationService).releaseOnce(
                "MFB-100", 100L, LocalDate.of(2026, 8, 20), 4);
    }

    @Test
    void businessFailureDoesNotReachProcessedTransition() {
        BookingFailedEvent event = event("booking-service", "evt-1");
        when(inboxDao.claim(
                "booking-service", "evt-1", "BookingFailedEvent", "MFB-100", "corr-1"))
                .thenReturn(1);
        when(reservationService.releaseOnce(
                "MFB-100", 100L, LocalDate.of(2026, 8, 20), 4))
                .thenThrow(new IllegalStateException("inventory update failed"));

        assertThrows(IllegalStateException.class, () -> processor.process(event));

        InOrder order = inOrder(inboxDao, reservationService);
        order.verify(inboxDao).claim(
                "booking-service", "evt-1", "BookingFailedEvent", "MFB-100", "corr-1");
        order.verify(reservationService).releaseOnce(
                "MFB-100", 100L, LocalDate.of(2026, 8, 20), 4);
        verify(inboxDao, never()).markProcessed(any(), any(), any());
    }

    private BookingFailedEvent event(String producer, String eventId) {
        BookingFailedEvent event = new BookingFailedEvent();
        event.setProducer(producer);
        event.setEventId(eventId);
        event.setCorrelationId("corr-1");
        event.setBookingId("MFB-100");
        event.setTimeSlotMapperId(100L);
        event.setBookingDate(LocalDate.of(2026, 8, 20));
        event.setGuestCount(4);
        return event;
    }
}
