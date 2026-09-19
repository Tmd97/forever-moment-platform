package com.forvmom.core.idempotency.event.consumer;

import com.forvmom.common.dto.events.BookingFailedEvent;
import com.forvmom.core.idempotency.event.model.EventProcessingResult;
import com.forvmom.core.idempotency.event.service.BookingFailureEventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Core service consumer for the {@code booking-failed} topic.
 *
 * <p>When the Booking Service cannot fulfill a booking, it publishes a
 * {@link BookingFailedEvent}. This adapter delegates the transactional inbox
 * claim and idempotent inventory release, then acknowledges the Kafka delivery
 * only after that work succeeds.
 */
@Component
public class BookingFailedConsumer {

    private static final Logger logger = LoggerFactory.getLogger(BookingFailedConsumer.class);

    private final BookingFailureEventProcessor bookingFailureEventProcessor;

    public BookingFailedConsumer(BookingFailureEventProcessor bookingFailureEventProcessor) {
        this.bookingFailureEventProcessor = bookingFailureEventProcessor;
    }

    /**
     * Processes one booking-failure delivery and acknowledges it only after the
     * inbox and reservation transaction commits successfully.
     */
    @KafkaListener(topics = "${kafka.topics.booking-failed}", groupId = "core-booking-failed-group", containerFactory = "kafkaListenerContainerFactory")
    public void onBookingFailed(
            @Payload BookingFailedEvent event,
            Acknowledgment acknowledgment
    ) {
        logger.info(
                "Received booking-failed: bookingId={}, eventId={}, correlationId={}, slotMapperId={}, bookingDate={}, guestCount={}",
                event.getBookingId(), event.getEventId(), event.getCorrelationId(),
                event.getTimeSlotMapperId(), event.getBookingDate(), event.getGuestCount());

        try {
            EventProcessingResult result =
                    bookingFailureEventProcessor.process(event);

            logger.info(
                    "Booking failure handled: bookingId={}, producer={}, eventId={}, result={}, slotMapperId={}, bookingDate={}",
                    event.getBookingId(), event.getProducer(), event.getEventId(), result,
                    event.getTimeSlotMapperId(), event.getBookingDate());

            acknowledgment.acknowledge();

        } catch (Exception exception) {
            logger.error("Failed to rollback inventory for bookingId={}: {}",
                    event.getBookingId(), exception.getMessage(), exception);
            // Do NOT ack — Kafka will redeliver → DLQ after max retries
            throw exception;
        }
    }
}
