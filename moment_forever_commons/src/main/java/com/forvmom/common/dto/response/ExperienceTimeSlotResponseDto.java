package com.forvmom.common.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for an ExperienceTimeSlotMapper record.
 * Combines TimeSlot master data with per-location configuration and pricing.
 */
public class ExperienceTimeSlotResponseDto {

    private Long mapperId;

    // TimeSlot master data
    private Long timeSlotId;
    private String name;
    private String startTime;
    private String endTime;

    private ExperienceLocationMapperDto experienceLocationMapperDto;

    // Per-location-experience overrides (Level 3 pricing)
    private BigDecimal priceOverride;
    private Integer maxCapacity;

    private LocalDate validFrom;
    private LocalDate validTo;
    private Boolean isActive;

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getMapperId() {
        return mapperId;
    }

    public void setMapperId(Long mapperId) {
        this.mapperId = mapperId;
    }

    public Long getTimeSlotId() {
        return timeSlotId;
    }

    public void setTimeSlotId(Long timeSlotId) {
        this.timeSlotId = timeSlotId;
    }


    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public BigDecimal getPriceOverride() {
        return priceOverride;
    }

    public void setPriceOverride(BigDecimal priceOverride) {
        this.priceOverride = priceOverride;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
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

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ExperienceLocationMapperDto getExperienceLocationMapperDto() {
        return experienceLocationMapperDto;
    }

    public void setExperienceLocationMapperDto(ExperienceLocationMapperDto experienceLocationMapperDto) {
        this.experienceLocationMapperDto = experienceLocationMapperDto;
    }
}
