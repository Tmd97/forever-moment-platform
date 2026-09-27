package com.forvmom.core.event_enrichment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forvmom.common.helpers.BookingOutboxPayload;
import com.forvmom.data.entities.BookingOutbox;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;

@Component
public class BookingPayloadParser {

    private final ObjectMapper objectMapper;

    public BookingPayloadParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public BookingOutboxPayload parse(BookingOutbox outbox) {
        try {
            Map<String, Object> payload = objectMapper.readValue(
                    outbox.getPayload(),
                    new TypeReference<Map<String, Object>>() {
                    }
            );

            return new BookingOutboxPayload.Builder()
                    .withUserId(toLong(payload.get("userId")))
                    .withSlotMapperId(toLong(payload.get("slotMapperId")))
                    .withGuestCount(toInt(payload.get("guestCount")))
                    .withBookingDate(LocalDate.parse((String) payload.get("bookingDate")))
                    .withPincode((String) payload.get("pincode"))
                    .withAddonMapperIds(payload.get("addonMapperIds") != null
                            ? toListLong((List<?>) payload.get("addonMapperIds"))
                            : Collections.emptyList())
                    .build();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse booking outbox payload for bookingReferenceId="
                            + outbox.getBookingReferenceId(),
                    e
            );
        }
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        return Long.parseLong(value.toString());
    }

    private Integer toInt(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        return Integer.parseInt(value.toString());
    }

    private List<Long> toListLong(List<?> rawValues) {
        List<Long> result = new ArrayList<>();

        for (Object rawValue : rawValues) {
            result.add(toLong(rawValue));
        }

        return result;
    }
}
