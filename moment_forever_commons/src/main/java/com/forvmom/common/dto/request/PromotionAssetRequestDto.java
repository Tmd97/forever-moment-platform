package com.forvmom.common.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class PromotionAssetRequestDto {

    @NotNull(message = "Media ID is required")
    private Long mediaId;

    @NotBlank(message = "Promotion key is required")
    @Size(max = 100, message = "Promotion key cannot exceed 100 characters")
    private String promoKey;

    @NotBlank(message = "Placement is required")
    @Size(max = 40, message = "Placement cannot exceed 40 characters")
    private String placement;

    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer priority = 100;
    private Boolean isActive = true;

    @Size(max = 120, message = "Title cannot exceed 120 characters")
    private String title;

    @Size(max = 300, message = "Alt text override cannot exceed 300 characters")
    private String altTextOverride;

    public Long getMediaId() {
        return mediaId;
    }

    public void setMediaId(Long mediaId) {
        this.mediaId = mediaId;
    }

    public String getPromoKey() {
        return promoKey;
    }

    public void setPromoKey(String promoKey) {
        this.promoKey = promoKey;
    }

    public String getPlacement() {
        return placement;
    }

    public void setPlacement(String placement) {
        this.placement = placement;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAltTextOverride() {
        return altTextOverride;
    }

    public void setAltTextOverride(String altTextOverride) {
        this.altTextOverride = altTextOverride;
    }
}
