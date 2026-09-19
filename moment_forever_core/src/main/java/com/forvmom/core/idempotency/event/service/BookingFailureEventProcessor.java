package com.forvmom.core.idempotency.event.service;

import com.forvmom.common.dto.events.BookingFailedEvent;
import com.forvmom.core.idempotency.event.model.EventProcessingResult;
import com.forvmom.core.idempotency.event.model.ProducerEventIdentity;
import com.forvmom.core.services.BookingReservationService;
import com.forvmom.data.dao.ConsumerEventInboxDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Applies booking-failure events exactly once at the business-effect boundary.
 *
 * <p>Modern events are deduplicated by producer and event ID in the durable
 * inbox. Legacy events without either identity remain supported and rely on
 * the reservation service's release-once guard.
 */
@Service
public class BookingFailureEventProcessor {

    private static final Logger logger =
            LoggerFactory.getLogger(BookingFailureEventProcessor.class);

    private final ConsumerEventInboxDao consumerEventInboxDao;
    private final BookingReservationService bookingReservationService;

    public BookingFailureEventProcessor(
            ConsumerEventInboxDao consumerEventInboxDao,
            BookingReservationService bookingReservationService
    ) {
        this.consumerEventInboxDao = consumerEventInboxDao;
        this.bookingReservationService = bookingReservationService;
    }

    /**
     * Claims the event identity, releases reserved capacity once, and marks the
     * inbox delivery processed in one transaction.
     */
    @Transactional
    public EventProcessingResult process(BookingFailedEvent event) {
        ProducerEventIdentity eventIdentity = validateIdentity(event);
        if (eventIdentity == null) {
            logger.debug(
                    "Processing legacy booking failure without event identity: bookingId={}",
                    event.getBookingId());
            return releaseReservation(event)
                    ? EventProcessingResult.PROCESSED
                    : EventProcessingResult.BUSINESS_NO_OP;
        }

        int claimCount = consumerEventInboxDao.claim(
                eventIdentity.producer(),
                eventIdentity.eventId(),
                event.getEventType(),
                event.getBookingId(),
                event.getCorrelationId());
        if (claimCount == 0) {
            logger.debug(
                    "Ignored duplicate booking failure event: producer={}, eventId={}",
                    eventIdentity.producer(),
                    eventIdentity.eventId());
            return EventProcessingResult.DUPLICATE_EVENT;
        }

        /*
         * The inbox identifies a delivery; the reservation state protects the
         * business effect. Distinct failure events remain processable while capacity
         * can still be released only once.
         */
        boolean released = releaseReservation(event);
        int processedCount = consumerEventInboxDao.markProcessed(
                eventIdentity.producer(),
                eventIdentity.eventId(),
                LocalDateTime.now());
        if (processedCount != 1) {
            throw new IllegalStateException(
                    "Could not mark consumer event as processed: producer="
                            + eventIdentity.producer() + ", eventId=" + eventIdentity.eventId());
        }
        return released ? EventProcessingResult.PROCESSED : EventProcessingResult.BUSINESS_NO_OP;
    }

    /**
     * Normalizes the compound event identity and rejects partially identified events.
     */
    private ProducerEventIdentity validateIdentity(BookingFailedEvent event) {
        String producer = normalize(event.getProducer());
        String eventId = normalize(event.getEventId());
        if (producer == null && eventId == null) {
            return null;
        }
        if (producer == null || eventId == null) {
            throw new IllegalArgumentException(
                    "BookingFailedEvent producer and eventId must either both be present or both be absent");
        }
        return new ProducerEventIdentity(producer, eventId);
    }

    /**
     * Converts blank identity components to {@code null}.
     */
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Delegates to the reservation-level idempotent capacity release.
     */
    private boolean releaseReservation(BookingFailedEvent event) {
        return bookingReservationService.releaseOnce(
                event.getBookingId(),
                event.getTimeSlotMapperId(),
                event.getBookingDate(),
                event.getGuestCount());
    }

}
