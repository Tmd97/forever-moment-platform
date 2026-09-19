package com.forvmom.core.idempotency.event.consumer;

import com.forvmom.common.dto.events.BookingFailedEvent;
import com.forvmom.core.idempotency.event.model.EventProcessingResult;
import com.forvmom.core.idempotency.event.service.BookingFailureEventProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingFailedConsumerTest {

    @Mock
    private BookingFailureEventProcessor bookingFailureEventProcessor;
    @Mock
    private Acknowledgment acknowledgment;

    private BookingFailedConsumer consumer;
    private BookingFailedEvent event;

    @BeforeEach
    void setUp() {
        consumer = new BookingFailedConsumer(bookingFailureEventProcessor);
        event = new BookingFailedEvent();
        event.setBookingId("MFB-100");
        event.setTimeSlotMapperId(100L);
        event.setBookingDate(LocalDate.of(2026, 8, 20));
        event.setGuestCount(4);
    }

    @Test
    void acknowledgesDuplicateAfterReservationServiceSkipsRelease() {
        when(bookingFailureEventProcessor.process(event))
                .thenReturn(EventProcessingResult.DUPLICATE_EVENT);

        consumer.onBookingFailed(event, acknowledgment);

        verify(acknowledgment).acknowledge();
    }

    @Test
    void doesNotAcknowledgeWhenReleaseTransactionFails() {
        when(bookingFailureEventProcessor.process(event))
                .thenThrow(new IllegalStateException("inventory update failed"));

        assertThrows(
                IllegalStateException.class,
                () -> consumer.onBookingFailed(event, acknowledgment));

        verify(acknowledgment, never()).acknowledge();
    }
}
