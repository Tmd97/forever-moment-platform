package com.forvmom.common.helpers;

import com.forvmom.common.dto.events.BookingRequestEvent;

import java.time.Instant;

public class BookingRequestEventBuilder {

    private String bookingReferenceId;
    private String eventId;
    private String producer;
    private Integer schemaVersion;
    private Instant occurredAt;
    private String correlationId;
    private String causationId;
    private BookingOutboxPayload payload;
    private BookingSnapshotBundle snapshots;
    private BookingPricingSummary pricing;

    public BookingRequestEventBuilder withBookingReferenceId(String bookingReferenceId) {
        this.bookingReferenceId = bookingReferenceId;
        return this;
    }

    public BookingRequestEventBuilder withEnvelope(
            String eventId,
            String producer,
            Integer schemaVersion,
            Instant occurredAt,
            String correlationId,
            String causationId
    ) {
        this.eventId = eventId;
        this.producer = producer;
        this.schemaVersion = schemaVersion;
        this.occurredAt = occurredAt;
        this.correlationId = correlationId;
        this.causationId = causationId;
        return this;
    }

    public BookingRequestEventBuilder withPayload(BookingOutboxPayload payload) {
        this.payload = payload;
        return this;
    }

    public BookingRequestEventBuilder withSnapshots(BookingSnapshotBundle snapshots) {
        this.snapshots = snapshots;
        return this;
    }

    public BookingRequestEventBuilder withPricing(BookingPricingSummary pricing) {
        this.pricing = pricing;
        return this;
    }

    public BookingRequestEvent build() {
        validate();

        BookingRequestEvent event = new BookingRequestEvent();

        event.setBookingId(bookingReferenceId);
        event.setEventId(eventId);
        event.setProducer(producer);
        event.setSchemaVersion(schemaVersion);
        event.setOccurredAt(occurredAt);
        event.setCorrelationId(correlationId);
        event.setCausationId(causationId);

        event.setUserId(snapshots.getUserSnapshot().getUserId());
        event.setUserEmail(snapshots.getUserSnapshot().getEmail());
        event.setUserFullName(snapshots.getUserSnapshot().getFullName());

        event.setExperienceId(snapshots.getExperienceSnapshot().getId());
        event.setExperienceName(snapshots.getExperienceSnapshot().getName());
        event.setExperienceSlug(snapshots.getExperienceSnapshot().getSlug());

        event.setLocationId(snapshots.getLocationSnapshot().getLocationId());
        event.setLocationName(snapshots.getLocationSnapshot().getLocationName());

        event.setTimeSlotMapperId(snapshots.getSlotSnapshot().getSlotMapperId());
        event.setTimeSlotId(snapshots.getSlotSnapshot().getTimeSlotId());
        event.setTimeSlotLabel(snapshots.getSlotSnapshot().getLabel());
        event.setStartTime(snapshots.getSlotSnapshot().getStartTime());
        event.setEndTime(snapshots.getSlotSnapshot().getEndTime());

        event.setBookingDate(payload.getBookingDate());
        event.setGuestCount(payload.getGuestCount());
        event.setPincode(payload.getPincode());

        event.setResolvedPricePerPerson(pricing.getResolvedPricePerPerson());
        event.setPricingLevel(pricing.getPricingLevel());
        event.setTotalAmount(pricing.getTotalAmount());
        event.setAddons(pricing.getBookedAddonSnapshots());
        event.setAddonsTotal(pricing.getAddonsTotal());
        event.setGrandTotal(pricing.getGrandTotal());

        return event;
    }

    private void validate() {
        if (bookingReferenceId == null || bookingReferenceId.isBlank()) {
            throw new IllegalStateException("bookingReferenceId is required");
        }
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalStateException("eventId is required");
        }
        if (producer == null || producer.isBlank() || schemaVersion == null || occurredAt == null) {
            throw new IllegalStateException("event envelope is incomplete");
        }

        if (payload == null) {
            throw new IllegalStateException("payload is required");
        }

        if (snapshots == null) {
            throw new IllegalStateException("snapshots are required");
        }

        if (pricing == null) {
            throw new IllegalStateException("pricing is required");
        }
    }
}
