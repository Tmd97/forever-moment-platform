package com.forvmom.core.event_enrichment;

import com.forvmom.common.dto.events.BookingRequestEvent;
import com.forvmom.common.helpers.BookingOutboxPayload;
import com.forvmom.common.helpers.BookingPricingSummary;
import com.forvmom.common.helpers.BookingRequestEventBuilder;
import com.forvmom.common.helpers.BookingSnapshotBundle;
import com.forvmom.data.entities.BookingOutbox;

import org.springframework.stereotype.Service;

@Service
public class BookingRequestEventFactory {

    public BookingRequestEvent create(
            BookingOutbox outbox,
            BookingOutboxPayload payload,
            BookingSnapshotBundle snapshots,
            BookingPricingSummary pricing
    ) {
            return new BookingRequestEventBuilder()
                    .withBookingReferenceId(outbox.getBookingReferenceId())
                .withEnvelope(
                        outbox.getEventId(),
                        outbox.getEventProducer(),
                        outbox.getSchemaVersion(),
                        outbox.getOccurredAt(),
                        outbox.getCorrelationId(),
                        outbox.getCausationId())
                .withPayload(payload)
                .withSnapshots(snapshots)
                .withPricing(pricing)
                .build();
    }
}
