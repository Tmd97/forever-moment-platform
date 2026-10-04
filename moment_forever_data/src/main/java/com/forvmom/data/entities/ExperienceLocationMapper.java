package com.forvmom.data.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Junction table: links an Experience to a Location with optional price
 * override.
 * ER: Experience ||--o{ ExperienceLocationMapper â€” Location ||--o{
 * ExperienceLocationMapper
 *
 * Pricing Level 2: price_override overrides Experience.base_price for this
 * location.
 * null price_override = fall back to Experience.base_price.
 */
@Entity
@Table(name = "experience_location_mappers")
@SQLDelete(sql = "UPDATE experience_location_mappers SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
public class ExperienceLocationMapper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experience_id", nullable = false)
    private Experience experience;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    /**
     * null = use Experience.base_price; non-null = location-level price override
     */
    @Column(name = "price_override", precision = 10, scale = 2)
    private BigDecimal priceOverride;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "deleted", nullable = false, columnDefinition = "boolean default false")
    private boolean deleted = false;

    @Column(name = "pincode_restriction_type", length = 20)
    private String pincodeRestrictionType = "WHITELIST";

    @CreationTimestamp
    @Column(name = "created_on", updatable = false)
    private Date createdOn;

    @UpdateTimestamp
    @Column(name = "updated_on")
    private Date updatedOn;

    /**
     * Timeslots attached to this experience-location combination.
     * LAZY â€” only JOIN FETCHed when needed.
     */
    @OneToMany(mappedBy = "experienceLocation", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ExperienceTimeSlotMapper> timeSlotMappers = new HashSet<>();

    /**
     * Pincode whitelist restricting where this experience-location attachment is
     * serviceable. Empty = unrestricted (serviceable at every pincode of the
     * location).
     * LAZY â€” only JOIN FETCHed when needed.
     */
    @OneToMany(mappedBy = "experienceLocationMapper", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ExperienceLocationPincodeMapper> pincodeMappers = new HashSet<>();

    // â”€â”€ Getters & Setters â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Experience getExperience() {
        return experience;
    }

    public void setExperience(Experience experience) {
        this.experience = experience;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public BigDecimal getPriceOverride() {
        return priceOverride;
    }

    public void setPriceOverride(BigDecimal priceOverride) {
        this.priceOverride = priceOverride;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public String getPincodeRestrictionType() {
        return pincodeRestrictionType;
    }

    public void setPincodeRestrictionType(String pincodeRestrictionType) {
        this.pincodeRestrictionType = pincodeRestrictionType;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public Date getUpdatedOn() {
        return updatedOn;
    }

    public Set<ExperienceTimeSlotMapper> getTimeSlotMappers() {
        return timeSlotMappers;
    }

    public void setTimeSlotMappers(Set<ExperienceTimeSlotMapper> timeSlotMappers) {
        this.timeSlotMappers = timeSlotMappers;
    }

    /** Bidirectional helper â€” adds a timeslot mapper and wires back-reference */
    public void addTimeSlotMapper(ExperienceTimeSlotMapper mapper) {
        timeSlotMappers.add(mapper);
        mapper.setExperienceLocation(this);
    }

    public Set<ExperienceLocationPincodeMapper> getPincodeMappers() {
        return pincodeMappers;
    }

    public void setPincodeMappers(Set<ExperienceLocationPincodeMapper> pincodeMappers) {
        this.pincodeMappers = pincodeMappers;
    }

    /** Bidirectional helper â€” adds a pincode restriction and wires back-reference */
    public void addPincodeMapper(ExperienceLocationPincodeMapper mapper) {
        pincodeMappers.add(mapper);
        mapper.setExperienceLocationMapper(this);
    }
}
