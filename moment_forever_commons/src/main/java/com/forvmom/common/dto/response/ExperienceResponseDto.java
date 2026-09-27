package com.forvmom.common.dto.response;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Full response DTO returned by GET /experiences/{id} and GET
 * /experiences/slug/{slug}.
 * Includes embedded ExperienceDetail, inclusions, cancellation policies,
 * locations with nested timeslots, and attached media gallery.
 * NOT used on list endpoints — use ExperienceHighlightResponseDto for lists.
 */
public class ExperienceResponseDto {

    private Long id;
    private String name;
    private String slug;
    private String tagName;
    private BigDecimal basePrice;
    private Integer displayOrder;
    private Boolean isFeatured;
    private Boolean isActive;

    // Denormalized sub-category + category fields
    private Long subCategoryId;
    private String subCategoryName;
    private Long categoryId;
    private String categoryName;

    // Embedded 1:1 detail — only populated on single-item fetch
    private ExperienceDetailResponseDto detail;

    // M:M embedded lists — only populated on single-item fetch
    private List<ExperienceInclusionResponseDto> inclusions;
    private List<CancellationPolicyResponseDto> cancellationPolicies;
    private List<ExperienceMediaResponseDto> media;

    /**
     * Locations attached to this experience, each carrying its nested timeslots.
     * Populated by ExperienceServiceImpl on the detail fetch (separate query).
     */
    private List<ExperienceLocationResponseDto> locations;

    private Date createdOn;
    private Date updatedOn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Long getSubCategoryId() {
        return subCategoryId;
    }

    public void setSubCategoryId(Long subCategoryId) {
        this.subCategoryId = subCategoryId;
    }

    public String getSubCategoryName() {
        return subCategoryName;
    }

    public void setSubCategoryName(String subCategoryName) {
        this.subCategoryName = subCategoryName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public ExperienceDetailResponseDto getDetail() {
        return detail;
    }

    public void setDetail(ExperienceDetailResponseDto detail) {
        this.detail = detail;
    }

    public List<ExperienceInclusionResponseDto> getInclusions() {
        return inclusions;
    }

    public void setInclusions(List<ExperienceInclusionResponseDto> inclusions) {
        this.inclusions = inclusions;
    }

    public List<CancellationPolicyResponseDto> getCancellationPolicies() {
        return cancellationPolicies;
    }

    public void setCancellationPolicies(List<CancellationPolicyResponseDto> cancellationPolicies) {
        this.cancellationPolicies = cancellationPolicies;
    }

    public List<ExperienceMediaResponseDto> getMedia() {
        return media;
    }

    public void setMedia(List<ExperienceMediaResponseDto> media) {
        this.media = media;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public Date getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(Date updatedOn) {
        this.updatedOn = updatedOn;
    }

    public List<ExperienceLocationResponseDto> getLocations() {
        return locations;
    }

    public void setLocations(List<ExperienceLocationResponseDto> locations) {
        this.locations = locations;
    }
}
