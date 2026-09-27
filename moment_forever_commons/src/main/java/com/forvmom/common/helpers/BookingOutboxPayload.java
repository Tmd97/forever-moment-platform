package com.forvmom.common.helpers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.LocalDate;

public class BookingOutboxPayload {

    private final Long userId;
    private final Long slotMapperId;
    private final Integer guestCount;
    private final String pincode;
    private final List<Long> addonMapperIds;
    private final LocalDate bookingDate;

    private BookingOutboxPayload(Builder builder) {
        this.userId = builder.userId;
        this.slotMapperId = builder.slotMapperId;
        this.guestCount = builder.guestCount;
        this.pincode = builder.pincode;
        this.addonMapperIds = builder.addonMapperIds == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(builder.addonMapperIds));
        this.bookingDate = builder.bookingDate;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getSlotMapperId() {
        return slotMapperId;
    }

    public Integer getGuestCount() {
        return guestCount;
    }

    public String getPincode() {
        return pincode;
    }

    public List<Long> getAddonMapperIds() {
        return addonMapperIds;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public static class Builder {

        private Long userId;
        private Long slotMapperId;
        private Integer guestCount;
        private String pincode;
        private List<Long> addonMapperIds;
        private LocalDate bookingDate;

        public Builder withUserId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder withSlotMapperId(Long slotMapperId) {
            this.slotMapperId = slotMapperId;
            return this;
        }

        public Builder withGuestCount(Integer guestCount) {
            this.guestCount = guestCount;
            return this;
        }

        public Builder withPincode(String pincode) {
            this.pincode = pincode;
            return this;
        }

        public Builder withAddonMapperIds(List<Long> addonMapperIds) {
            this.addonMapperIds = addonMapperIds;
            return this;
        }

        public Builder withBookingDate(LocalDate bookingDate) {
            this.bookingDate = bookingDate;
            return this;
        }

        public BookingOutboxPayload build() {
            validate();
            return new BookingOutboxPayload(this);
        }

        private void validate() {
            if (userId == null) {
                throw new IllegalStateException("userId is required in booking outbox payload");
            }

            if (slotMapperId == null) {
                throw new IllegalStateException("slotMapperId is required in booking outbox payload");
            }

            if (guestCount == null || guestCount <= 0) {
                throw new IllegalStateException("guestCount must be greater than zero");
            }

            if (bookingDate == null) {
                throw new IllegalStateException("bookingDate is required in booking outbox payload");
            }
        }
    }
}
