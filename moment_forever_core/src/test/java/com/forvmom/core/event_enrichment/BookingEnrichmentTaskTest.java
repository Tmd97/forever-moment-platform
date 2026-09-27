package com.forvmom.core.event_enrichment;

import com.forvmom.common.dto.events.BookingRequestEvent;
import com.forvmom.common.helpers.BookingOutboxPayload;
import com.forvmom.common.helpers.BookingPricingSummary;
import com.forvmom.common.helpers.BookingSnapshotBundle;
import com.forvmom.core.retries.model.BookingOutboxProcessingClaim;
import com.forvmom.core.retries.service.BookingOutboxPublicationTransactionService;
import com.forvmom.core.retries.service.BookingOutboxStateService;
import com.forvmom.data.entities.BookingOutbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingEnrichmentTaskTest {

    private static final String BOOKING_REFERENCE = "MFB-100";

    @Mock
    private BookingPayloadParser payloadParser;
    @Mock
    private BookingSnapshotResolver snapshotResolver;
    @Mock
    private BookingPricingCalculator pricingCalculator;
    @Mock
    private BookingRequestEventFactory eventFactory;
    @Mock
    private BookingOutboxStateService stateService;
    @Mock
    private BookingOutboxPublicationTransactionService publicationTransactionService;

    private BookingEnrichmentTask task;
    private BookingRequestEvent event;
    private BookingOutboxProcessingClaim claim;

    @BeforeEach
    void setUp() {
        task = new BookingEnrichmentTask(
                payloadParser,
                snapshotResolver,
                pricingCalculator,
                eventFactory,
                stateService,
                publicationTransactionService);

        BookingOutbox outbox = new BookingOutbox();
        outbox.setStatus(BookingOutbox.STATUS_PROCESSING);
        outbox.setBookingReferenceId(BOOKING_REFERENCE);
        outbox.setProcessingOwnerToken("owner-1");
        outbox.setEventId("evt-stable-1");
        outbox.setEventProducer("moment-forever-core");
        outbox.setSchemaVersion(1);
        outbox.setOccurredAt(Instant.parse("2026-08-20T10:00:00Z"));
        outbox.setCorrelationId("corr-1");
        BookingOutboxPayload payload = mock(BookingOutboxPayload.class);
        BookingSnapshotBundle snapshots = mock(BookingSnapshotBundle.class);
        BookingPricingSummary pricing = mock(BookingPricingSummary.class);
        event = new BookingRequestEvent();
        event.setBookingId(BOOKING_REFERENCE);
        claim = new BookingOutboxProcessingClaim(BOOKING_REFERENCE, "owner-1");

        when(stateService.loadRequired(BOOKING_REFERENCE)).thenReturn(outbox);
        when(stateService.claimForProcessing(BOOKING_REFERENCE)).thenReturn(claim);
        when(payloadParser.parse(outbox)).thenReturn(payload);
        when(snapshotResolver.resolve(payload)).thenReturn(snapshots);
        when(pricingCalculator.calculate(payload, snapshots)).thenReturn(pricing);
        when(eventFactory.create(outbox, payload, snapshots, pricing)).thenReturn(event);
    }

    @Test
    void marksPublishedOnlyAfterKafkaAcknowledges() {
        when(publicationTransactionService.publish(claim, event)).thenReturn(true);

        task.enrich(BOOKING_REFERENCE);

        verify(publicationTransactionService).publish(claim, event);
        verify(stateService, never()).markFailed(
                eq(claim),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void recordsKafkaFailureInsteadOfMarkingPublished() {
        when(publicationTransactionService.publish(claim, event))
                .thenThrow(new IllegalStateException("broker unavailable"));

        task.enrich(BOOKING_REFERENCE);

        verify(stateService).markFailed(
                eq(claim),
                contains("broker unavailable"));
    }

    @Test
    void staleWorkerStopsWithoutMutatingNewOwnerState() {
        when(publicationTransactionService.publish(claim, event)).thenReturn(false);

        task.enrich(BOOKING_REFERENCE);

        verify(stateService, never()).markFailed(
                eq(claim),
                org.mockito.ArgumentMatchers.anyString());
    }
}
