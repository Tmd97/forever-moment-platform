package com.forvmom.core.event_enrichment;

import com.forvmom.common.dto.events.BookingRequestEvent;
import com.forvmom.common.helpers.BookingOutboxPayload;
import com.forvmom.common.helpers.BookingPricingSummary;
import com.forvmom.common.helpers.BookingSnapshotBundle;
import com.forvmom.data.entities.BookingOutbox;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

class BookingRequestEventFactoryTest {

    @Test
    void retryRebuildsEventWithPersistedIdentity() {
        BookingOutbox outbox = new BookingOutbox();
        outbox.setBookingReferenceId("MFB-100");
        outbox.setEventId("evt-stable-1");
        outbox.setEventProducer("moment-forever-core");
        outbox.setSchemaVersion(1);
        outbox.setOccurredAt(Instant.parse("2026-08-20T10:00:00Z"));
        outbox.setCorrelationId("corr-1");

        BookingOutboxPayload payload = mock(BookingOutboxPayload.class);
        BookingSnapshotBundle snapshots =
                mock(BookingSnapshotBundle.class, RETURNS_DEEP_STUBS);
        BookingPricingSummary pricing = mock(BookingPricingSummary.class);
        BookingRequestEventFactory factory = new BookingRequestEventFactory();

        BookingRequestEvent first = factory.create(outbox, payload, snapshots, pricing);
        BookingRequestEvent retry = factory.create(outbox, payload, snapshots, pricing);

        assertEquals(first.getEventId(), retry.getEventId());
        assertEquals("evt-stable-1", retry.getEventId());
        assertEquals(first.getOccurredAt(), retry.getOccurredAt());
        assertEquals("MFB-100", retry.getBookingId());
    }
}
