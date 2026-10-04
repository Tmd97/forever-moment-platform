package com.forvmom.common.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for a location attached to an experience
 * (ExperienceLocationMapper row).
 * Flattens location master data + per-experience overrides + nested timeslots.
 *
 * Used in ExperienceResponseDto.locations for the detail endpoint.
 */
public class ExperienceLocationResponseDto {

    // Junction identifier
    private Long mapperId;

    // Location master data
    private Long locationId;
    private String locationName;
    private String city;
    private String state;
    private String country;
    private String address;
    private Double latitude;
    private Double longitude;

    // Per-experience price override (Level 2 pricing â€” overrides
    // Experience.basePrice)
    private BigDecimal priceOverride;

    private LocalDate validFrom;
    private LocalDate validTo;
    private Boolean isActive;
    private String pincodeRestrictionType;

    // Nested timeslots attached under this experience-location pair
    private List<ExperienceTimeSlotResponseDto> timeslots;

    // â”€â”€ Getters & Setters â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public Long getMapperId() {
        return mapperId;
    }

    public void setMapperId(Long mapperId) {
        this.mapperId = mapperId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getPriceOverride() {
        return priceOverride;
    }

    public void setPriceOverride(BigDecimal priceOverride) {
        this.priceOverride = priceOverride;
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

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public String getPincodeRestrictionType() {
        return pincodeRestrictionType;
    }

    public void setPincodeRestrictionType(String pincodeRestrictionType) {
        this.pincodeRestrictionType = pincodeRestrictionType;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public List<ExperienceTimeSlotResponseDto> getTimeslots() {
        return timeslots;
    }

    public void setTimeslots(List<ExperienceTimeSlotResponseDto> timeslots) {
        this.timeslots = timeslots;
    }
}
