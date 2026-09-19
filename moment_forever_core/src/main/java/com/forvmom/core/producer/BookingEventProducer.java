package com.forvmom.core.producer;

import com.forvmom.common.dto.events.BookingRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer responsible for publishing {@link BookingRequestEvent}s to the
 * {@code booking-requested} topic.
 *
 * <p>
 * Responsibilities:
 * <ul>
 * <li>Stamp the {@code requestedAt} timestamp</li>
 * <li>Send the event keyed by {@code bookingId} so all events for one booking
 * share a partition</li>
 * <li>Return the broker acknowledgement future to the caller</li>
 * </ul>
 *
 * <p>
 * All business validation and price resolution happen upstream — capacity is
 * reserved by {@code BookingOrchestrationService} and the event is enriched by
 * {@code BookingEnrichmentTask} before this class is called.
 *
 */
@Service
public class BookingEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(BookingEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.booking-requested}")
    private String bookingRequestedTopic;

    public BookingEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Stamps the event and publishes it to the {@code booking-requested} topic.
     *
     * <p>
     * The caller must wait for the returned future before marking the outbox record
     * as published.
     *
     * @param event the fully enriched booking event
     * @return future completed by Kafka after broker acknowledgement
     */
    public CompletableFuture<SendResult<String, Object>> sendBookingRequested(BookingRequestEvent event) {
        String bookingId = event.getBookingId();
        if (event.getOccurredAt() != null) {
            event.setRequestedAt(LocalDateTime.ofInstant(event.getOccurredAt(), ZoneOffset.UTC));
        }

        logger.info("Publishing booking-requested: bookingId={}, eventId={}, correlationId={}, userId={}, experienceId={}, "
                + "slotMapperId={}, date={}, guests={}, grandTotal={}",
                bookingId,
                event.getEventId(),
                event.getCorrelationId(),
                event.getUserId(),
                event.getExperienceId(),
                event.getTimeSlotMapperId(),
                event.getBookingDate(),
                event.getGuestCount(),
                event.getGrandTotal());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(bookingRequestedTopic, bookingId, event);
        future.whenComplete((result, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to publish booking-requested: bookingId={}, eventId={}, correlationId={}, error={}",
                                bookingId, event.getEventId(), event.getCorrelationId(), ex.getMessage(), ex);
                    } else {
                        logger.info("Successfully published booking-requested: bookingId={}, eventId={}, correlationId={}, "
                                + "topic={}, partition={}, offset={}",
                                bookingId,
                                event.getEventId(),
                                event.getCorrelationId(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
        return future;
    }
}