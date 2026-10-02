package com.forvmom.data.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import java.util.Date;

/**
 * Junction table restricting an {@link ExperienceLocationMapper} (an
 * experience attached to a location) to a subset of that location's
 * {@link Pincode}s.
 * ER: ExperienceLocationMapper ||--o{ ExperienceLocationPincodeMapper —
 * Pincode ||--o{ ExperienceLocationPincodeMapper
 *
 * <p>
 * No rows for a given mapper = unrestricted (serviceable at every pincode of
 * the mapper's location) — this keeps existing experience-location
 * attachments backward compatible. One or more rows = whitelist: the
 * experience is only serviceable at the listed pincodes.
 */
@Entity
@Table(name = "experience_location_pincodes")
@SQLDelete(sql = "UPDATE experience_location_pincodes SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
public class ExperienceLocationPincodeMapper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experience_location_mapper_id", nullable = false)
    private ExperienceLocationMapper experienceLocationMapper;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pincode_id", nullable = false)
    private Pincode pincode;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "deleted", nullable = false, columnDefinition = "boolean default false")
    private boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_on", updatable = false)
    private Date createdOn;

    @UpdateTimestamp
    @Column(name = "updated_on")
    private Date updatedOn;

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExperienceLocationMapper getExperienceLocationMapper() {
        return experienceLocationMapper;
    }

    public void setExperienceLocationMapper(ExperienceLocationMapper experienceLocationMapper) {
        this.experienceLocationMapper = experienceLocationMapper;
    }

    public Pincode getPincode() {
        return pincode;
    }

    public void setPincode(Pincode pincode) {
        this.pincode = pincode;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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
}
