package com.forvmom.common.dto.response;

import java.util.ArrayList;
import java.util.List;

public class CategoryResponseDto extends NamedEntityDto implements CatalogMediaCarrier {

    private String description;
    private String slug;
    private String icon;
    private Long mediaId;
    private String fileName;
    private String storageFileName;
    private String altText;
    private String heroUrl;
    private String thumbnailUrl;
    private String originalUrl;
    private Long displayOrder;
    private Boolean isActive;
    private List<SubCategoryResponseDto> subCategories = new ArrayList<>();
    private List<ExperienceMediaResponseDto> media = new ArrayList<>();

    public CategoryResponseDto() {}

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
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

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
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

    public Long getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Long displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public List<SubCategoryResponseDto> getSubCategories() {
        return subCategories;
    }

    public void setSubCategories(List<SubCategoryResponseDto> subCategories) {
        this.subCategories = subCategories;
    }

    public List<ExperienceMediaResponseDto> getMedia() {
        return media;
    }

    public void setMedia(List<ExperienceMediaResponseDto> media) {
        this.media = media;
    }
}
