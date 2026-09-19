package com.forvmom.core.idempotency.request.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forvmom.common.dto.request.BookingRequestDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BookingRequestFingerprintServiceTest {

    private final BookingRequestFingerprintService service =
            new BookingRequestFingerprintService(new ObjectMapper());

    @Test
    void normalizesNullAndEmptyAddonsAndIgnoresAddonOrder() {
        BookingRequestDto first = request(2, List.of(9L, 3L));
        BookingRequestDto reordered = request(2, List.of(3L, 9L));

        assertEquals(
                service.createFingerprint(first),
                service.createFingerprint(reordered));

        BookingRequestDto withoutAddons = request(2, null);
        BookingRequestDto emptyAddons = request(2, List.of());
        assertEquals(
                service.createFingerprint(withoutAddons),
                service.createFingerprint(emptyAddons));
    }

    @Test
    void changesWhenRequestMeaningChanges() {
        assertNotEquals(
                service.createFingerprint(request(2, List.of(3L))),
                service.createFingerprint(request(3, List.of(3L))));
    }

    private BookingRequestDto request(int guests, List<Long> addonIds) {
        BookingRequestDto request = new BookingRequestDto();
        request.setTimeSlotMapperId(100L);
        request.setBookingDate(LocalDate.of(2026, 8, 24));
        request.setGuestCount(guests);
        request.setPincode("560001");
        request.setAddonMapperIds(addonIds);
        return request;
    }
}
