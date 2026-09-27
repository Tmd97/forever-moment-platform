package com.forvmom.common.dto.response;

import java.math.BigDecimal;

/**
 * Response DTO representing an addon as it is attached to a specific
 * experience.
 *
 * <p>
 * Exposes the {@code ExperienceAddonMapper} junction data (mapperId,
 * effectivePrice, isFree) alongside flat addon master fields — so the UI
 * has a single clean object to work with.
 *
 * <p>
 * The {@code mapperId} field is the {@code ExperienceAddonMapper.id} and
 * should be used as {@code addonMapperIds} in the booking request.
 */
public class ExperienceAddonResponseDto {

    /**
     * ExperienceAddonMapper.id — use this as addonMapperId in the booking request
     */
    private Long mapperId;

    /** Master Addon.id */
    private Long addonId;
    private Long mediaId;

    private String name;
    private String description;
    private String icon;
    private String heroUrl;
    private String thumbnailUrl;
    private String originalUrl;

    /** Addon.basePrice — the global default price */
    private BigDecimal basePrice;

    /**
     * Per-experience price override.
     * {@code null} = use {@code basePrice}.
     */
    private BigDecimal priceOverride;

    /**
     * Resolved final price for this experience:
     * {@code isFree=true → 0}, {@code priceOverride != null → priceOverride},
     * else {@code basePrice}.
     */
    private BigDecimal effectivePrice;

    /** {@code true} = addon is complimentary for this experience */
    private Boolean isFree;

    private Boolean isActive;

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getMapperId() {
        return mapperId;
    }

    public void setMapperId(Long mapperId) {
        this.mapperId = mapperId;
    }

    public Long getAddonId() {
        return addonId;
    }

    public void setAddonId(Long addonId) {
        this.addonId = addonId;
    }

    public Long getMediaId() {
        return mediaId;
    }

    public void setMediaId(Long mediaId) {
        this.mediaId = mediaId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getHeroUrl() {
        return heroUrl;
    }

    public void setHeroUrl(String heroUrl) {
        this.heroUrl = heroUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getPriceOverride() {
        return priceOverride;
    }

    public void setPriceOverride(BigDecimal priceOverride) {
        this.priceOverride = priceOverride;
    }

    public BigDecimal getEffectivePrice() {
        return effectivePrice;
    }

    public void setEffectivePrice(BigDecimal effectivePrice) {
        this.effectivePrice = effectivePrice;
    }

    public Boolean getIsFree() {
        return isFree;
    }

    public void setIsFree(Boolean isFree) {
        this.isFree = isFree;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
