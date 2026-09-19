package com.forvmom.data.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_reservation")
public class BookingReservation {

    public static final String STATUS_RESERVED = "RESERVED";
    public static final String STATUS_RELEASED = "RELEASED";

    @Id
    @Column(name = "booking_reference_id", nullable = false, length = 60)
    private String bookingReferenceId;

    @Column(name = "slot_mapper_id", nullable = false)
    private Long slotMapperId;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "guest_count", nullable = false)
    private Integer guestCount;

    @Column(name = "status", nullable = false, length = 20)
    private String status = STATUS_RESERVED;

    @CreationTimestamp
    @Column(name = "reserved_at", nullable = false, updatable = false)
    private LocalDateTime reservedAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public String getBookingReferenceId() {
        return bookingReferenceId;
    }

    public void setBookingReferenceId(String bookingReferenceId) {
        this.bookingReferenceId = bookingReferenceId;
    }

    public Long getSlotMapperId() {
        return slotMapperId;
    }

    public void setSlotMapperId(Long slotMapperId) {
        this.slotMapperId = slotMapperId;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public Integer getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(Integer guestCount) {
        this.guestCount = guestCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getReservedAt() {
        return reservedAt;
    }

    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }

    public Long getVersion() {
        return version;
    }
}
