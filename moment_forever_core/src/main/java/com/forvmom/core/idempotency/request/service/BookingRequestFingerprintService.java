package com.forvmom.core.idempotency.request.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forvmom.common.dto.request.BookingRequestDto;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Creates a deterministic fingerprint of booking fields that affect the
 * booking-creation result.
 */
@Service
public class BookingRequestFingerprintService {

    private final ObjectMapper objectMapper;

    public BookingRequestFingerprintService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Canonicalizes the request and returns its SHA-256 fingerprint.
     *
     * <p>Add-on order is normalized because it does not change booking semantics.
     */
    public String createFingerprint(BookingRequestDto bookingRequest) {
        Map<String, Object> normalizedRequest = new TreeMap<>();
        normalizedRequest.put(
                "addonMapperIds",
                normalizeAddonMapperIds(bookingRequest.getAddonMapperIds()));
        normalizedRequest.put("bookingDate", bookingRequest.getBookingDate().toString());
        normalizedRequest.put("guestCount", bookingRequest.getGuestCount());
        normalizedRequest.put("pincode", bookingRequest.getPincode());
        normalizedRequest.put("timeSlotMapperId", bookingRequest.getTimeSlotMapperId());

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(normalizedRequest);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Could not fingerprint booking request", exception);
        }
    }

    /**
     * Returns add-on identifiers in a stable order for canonical serialization.
     */
    private List<Long> normalizeAddonMapperIds(List<Long> addonMapperIds) {
        if (addonMapperIds == null || addonMapperIds.isEmpty()) {
            return List.of();
        }
        List<Long> normalized = new ArrayList<>(addonMapperIds);
        normalized.sort(Long::compareTo);
        return normalized;
    }
}
