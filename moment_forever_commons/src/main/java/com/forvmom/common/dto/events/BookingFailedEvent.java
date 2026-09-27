package com.forvmom.common.dto.events;

import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * Published to topic {@code booking-failed} by the Booking Service when a
 * booking cannot be fulfilled (payment failure or validation failure).
 *
 * <p>
 * Consumed by Core Service to release the date-scoped inventory reserved during
 * the booking request transaction.
 *
 * <p>
 * IMPORTANT: {@code timeSlotMapperId}, {@code bookingDate}, and
 * {@code guestCount} identify the inventory row and quantity to release.
 */
public class BookingFailedEvent extends BaseEvent {

    private String bookingId;
    private Long userId;
    private String userEmail;
    private Long experienceId;
    /** Required for Core to identify the configured slot mapper. */
    private Long timeSlotMapperId;
    /** Required for Core to know how much capacity to release. */
    private Integer guestCount;
    private LocalDate bookingDate;
    private String failureReason;
    private LocalDateTime failedAt;

    public BookingFailedEvent() {
        super();
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

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(LocalDateTime failedAt) {
        this.failedAt = failedAt;
    }
}
