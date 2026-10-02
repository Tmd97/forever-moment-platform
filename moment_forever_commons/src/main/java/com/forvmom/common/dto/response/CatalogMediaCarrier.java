package com.forvmom.common.dto.response;

/**
 * Common contract implemented by read-model DTOs that carry a hydrated
 * primary image (CategoryResponseDto, SubCategoryResponseDto, ...).
 * Lets a single bulk-enrichment routine (see CatalogMediaService) populate
 * media fields for any of them instead of duplicating the logic per DTO.
 */
public interface CatalogMediaCarrier {
    long getId();

    void setMediaId(Long mediaId);

    void setFileName(String fileName);

    void setStorageFileName(String storageFileName);

    void setAltText(String altText);

    String getHeroUrl();

    void setHeroUrl(String heroUrl);

    String getThumbnailUrl();

    void setThumbnailUrl(String thumbnailUrl);

    String getOriginalUrl();

    void setOriginalUrl(String originalUrl);

    void setIcon(String icon);
}
