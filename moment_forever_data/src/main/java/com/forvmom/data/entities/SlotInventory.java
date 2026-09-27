package com.forvmom.data.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "slot_inventory",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_slot_inventory_mapper_date",
                        columnNames = {"slot_mapper_id", "booking_date"})
        })
@Check(constraints = "booked_count >= 0")
public class SlotInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_mapper_id", nullable = false)
    private ExperienceTimeSlotMapper slotMapper;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "booked_count", nullable = false)
    private Integer bookedCount = 0;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @UpdateTimestamp
    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    public Long getId() {
        return id;
    }

    public ExperienceTimeSlotMapper getSlotMapper() {
        return slotMapper;
    }

    public void setSlotMapper(ExperienceTimeSlotMapper slotMapper) {
        this.slotMapper = slotMapper;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public Integer getBookedCount() {
        return bookedCount;
    }

    public void setBookedCount(Integer bookedCount) {
        this.bookedCount = bookedCount;
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public LocalDateTime getUpdatedOn() {
        return updatedOn;
    }
}
