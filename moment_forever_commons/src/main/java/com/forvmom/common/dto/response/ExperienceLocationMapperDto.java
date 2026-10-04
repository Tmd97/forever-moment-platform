package com.forvmom.common.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ExperienceLocationMapperDto {

    private Long id;
    private Long experienceId;
    private Long locationId;

    private BigDecimal priceOverride;

    private Boolean isActive;

    private LocalDate validFrom;
    private LocalDate validTo;
    private String pincodeRestrictionType;

    // Getters & Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExperienceId() {
        return experienceId;
    }

    public void setExperienceId(Long experienceId) {
        this.experienceId = experienceId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
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

    public void setIsActive(Boolean active) {
        isActive = active;
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
}