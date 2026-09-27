package com.forvmom.common.dto.response;

/**
 * Response DTO for an ExperienceMediaMapper record.
 * Combines the junction-row metadata with the public URL built from
 * storageFileName.
 */
public class ExperienceMediaResponseDto {

    private Long mapperId;

    // Media master fields
    private Long mediaId;
    private String fileName;
    private String storageFileName;
    private String mimeType;
    private Long fileSizeBytes;

    // Public URLs (built via ImageUrlConfig from storageFileName — cache-busted by
    // design)
    private String url;
    private String heroUrl;
    private String thumbnailUrl;
    private String originalUrl;

    // Junction-row fields
    private Integer displayOrder;
    private Boolean isPrimary;
    private String altText;
    private Boolean isActive;

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getMapperId() {
        return mapperId;
    }

    public void setMapperId(Long mapperId) {
        this.mapperId = mapperId;
    }

    public Long getMediaId() {
        return mediaId;
    }

    public void setMediaId(Long mediaId) {
        this.mediaId = mediaId;
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

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
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

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
