package com.forvmom.data.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "experience")
@SQLDelete(sql = "UPDATE experience SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
public class Experience extends NamedEntity {

    @Column(name = "slug", nullable = false, unique = true, length = 220)
    private String slug;

    @Column(name = "tag_name", length = 100)
    private String tagName;

    @Column(name = "base_price", precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "is_featured")
    private Boolean isFeatured = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id", nullable = false)
    private SubCategory subCategory;

    @OneToOne(mappedBy = "experience", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private ExperienceDetail detail;

    /**
     * Junction rows linking this experience to reusable inclusion items.
     * LAZY â€” only loaded when explicitly JOIN FETCHed (detail endpoint).
     */
    @OneToMany(mappedBy = "experience", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ExperienceInclusionMapper> inclusionMappers = new HashSet<>();

    /**
     * Junction rows linking this experience to reusable cancellation policy points.
     * LAZY â€” only loaded when explicitly JOIN FETCHed (detail endpoint).
     */
    @OneToMany(mappedBy = "experience", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ExperienceCancellationPolicyMapper> policyMappers = new HashSet<>();

    /**
     * Junction rows linking this experience to reusable add-on items.
     * LAZY â€” not loaded on list endpoints.
     */
    @OneToMany(mappedBy = "experience", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ExperienceAddonMapper> addonMappers = new HashSet<>();

    /**
     * Junction rows linking this experience to locations (each with optional price
     * override).
     * LAZY â€” only JOIN FETCHed on the detail endpoint (Query 3 of 3).
     */
    @OneToMany(mappedBy = "experience", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ExperienceLocationMapper> locationMappers = new HashSet<>();

    /**
     * Junction rows linking this experience to Media (images/videos).
     * LAZY â€” only JOIN FETCHed on the detail/gallery endpoint.
     */
    @OneToMany(mappedBy = "experience", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ExperienceMediaMapper> mediaMappers = new HashSet<>();

    @OneToMany(mappedBy = "experience", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ExperienceCouponMapper> couponMappers = new HashSet<>();

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

    public SubCategory getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(SubCategory subCategory) {
        this.subCategory = subCategory;
    }

    public ExperienceDetail getDetail() {
        return detail;
    }

    public void setDetail(ExperienceDetail detail) {
        this.detail = detail;
        if (detail != null)
            detail.setExperience(this);
    }

    public Set<ExperienceInclusionMapper> getInclusionMappers() {
        return inclusionMappers;
    }

    public void setInclusionMappers(Set<ExperienceInclusionMapper> inclusionMappers) {
        this.inclusionMappers = inclusionMappers;
    }

    public Set<ExperienceCancellationPolicyMapper> getPolicyMappers() {
        return policyMappers;
    }

    public void setPolicyMappers(Set<ExperienceCancellationPolicyMapper> policyMappers) {
        this.policyMappers = policyMappers;
    }

    public void addInclusionMapper(ExperienceInclusionMapper mapper) {
        inclusionMappers.add(mapper);
        mapper.setExperience(this);
    }

    /**
     * Adds a policy mapper and wires both sides of the relationship.
     * Call this instead of manually setting experience on the mapper.
     */
    public void addPolicyMapper(ExperienceCancellationPolicyMapper mapper) {
        policyMappers.add(mapper);
        mapper.setExperience(this);
    }

    /**
     * Adds an addon mapper and wires both sides of the relationship.
     */
    public void addAddonMapper(ExperienceAddonMapper mapper) {
        addonMappers.add(mapper);
        mapper.setExperience(this);
    }

    public Set<ExperienceAddonMapper> getAddonMappers() {
        return addonMappers;
    }

    public void setAddonMappers(Set<ExperienceAddonMapper> addonMappers) {
        this.addonMappers = addonMappers;
    }

    public Set<ExperienceLocationMapper> getLocationMappers() {
        return locationMappers;
    }

    public void setLocationMappers(Set<ExperienceLocationMapper> locationMappers) {
        this.locationMappers = locationMappers;
    }

    /**
     * Adds a location mapper and wires both sides of the bidirectional
     * relationship.
     */
    public void addLocationMapper(ExperienceLocationMapper mapper) {
        locationMappers.add(mapper);
        mapper.setExperience(this);
    }

    public Set<ExperienceMediaMapper> getMediaMappers() {
        return mediaMappers;
    }

    public void setMediaMappers(Set<ExperienceMediaMapper> mediaMappers) {
        this.mediaMappers = mediaMappers;
    }

    /**
     * Adds a media mapper and wires both sides of the bidirectional relationship.
     */
    public Set<ExperienceCouponMapper> getCouponMappers() {
        return couponMappers;
    }

    public void setCouponMappers(Set<ExperienceCouponMapper> couponMappers) {
        this.couponMappers = couponMappers;
    }

    public void addCouponMapper(ExperienceCouponMapper mapper) {
        couponMappers.add(mapper);
        mapper.setExperience(this);
    }

    public void addMediaMapper(ExperienceMediaMapper mapper) {
        mediaMappers.add(mapper);
        mapper.setExperience(this);
    }
}
