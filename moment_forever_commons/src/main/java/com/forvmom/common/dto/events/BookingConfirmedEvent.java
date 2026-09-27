package com.forvmom.common.dto.events;

import java.time.LocalDateTime;

/**
 * Published to topic {@code booking-confirmed} by the Booking Service when a
 * booking is successfully confirmed (after payment or direct confirmation).
 * Consumed by Core Service (informational — inventory already incremented).
 */
public class BookingConfirmedEvent extends BaseEvent {

    private String bookingId;
    private Long userId;
    private String userEmail;
    private Long experienceId;
    private Long timeSlotMapperId;
    private Integer guestCount;
    private LocalDateTime confirmedAt;

    public BookingConfirmedEvent() {
        super();
        setEventType("BOOKING_CONFIRMED");
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public Long getExperienceId() {
        return experienceId;
    }

    public void setExperienceId(Long experienceId) {
        this.experienceId = experienceId;
    }

    public Long getTimeSlotMapperId() {
        return timeSlotMapperId;
    }

    public void setTimeSlotMapperId(Long timeSlotMapperId) {
        this.timeSlotMapperId = timeSlotMapperId;
    }

    public Integer getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(Integer guestCount) {
        this.guestCount = guestCount;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }
}
