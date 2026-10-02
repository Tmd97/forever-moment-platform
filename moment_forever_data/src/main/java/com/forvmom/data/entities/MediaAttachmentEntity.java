package com.forvmom.data.entities;

/**
 * Common contract implemented by every "X media mapper" junction entity
 * (ExperienceMediaMapper, CategoryMediaMapper, SubCategoryMediaMapper, ...).
 * Lets shared mapping/enrichment code (see CatalogMediaBeanMapper) work
 * against one type instead of being copy-pasted per owning entity.
 */
public interface MediaAttachmentEntity {
    Long getId();

    Media getMedia();

    Integer getDisplayOrder();

    Boolean getIsPrimary();

    String getAltText();

    Boolean getIsActive();
}
