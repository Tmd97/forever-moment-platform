package com.forvmom.common.dto.response;

import java.time.LocalDateTime;

public class PromotionImageResponseDto {

    private Long id;
    private Long mediaId;
    private String promoKey;
    private String placement;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer priority;
    private Boolean isActive;
    private String title;
    private String altTextOverride;
    private String fileName;
    private String storageFileName;
    private String url;
    private String heroUrl;
    private String thumbnailUrl;
    private String originalUrl;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getStorageFileName() {
        return storageFileName;
    }

    public void setStorageFileName(String storageFileName) {
        this.storageFileName = storageFileName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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
}
