package com.forvmom.data.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Master record for an add-on item.
 * One Addon can be attached to multiple experiences via ExperienceAddonMapper.
 */
@Entity
@Table(name = "addons")
@SQLDelete(sql = "UPDATE addons SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
public class Addon extends NamedEntity {

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon")
    private String icon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id")
    private Media imageMedia;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "deleted", nullable = false, columnDefinition = "boolean default false")
    private boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_on", updatable = false)
    private Date createdOn;

    @UpdateTimestamp
    @Column(name = "updated_on")
    private Date updatedOn;

    @OneToMany(mappedBy = "addon", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ExperienceAddonMapper> addonMappers = new HashSet<>();


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

    public Media getImageMedia() {
        return imageMedia;
    }

    public void setImageMedia(Media imageMedia) {
        this.imageMedia = imageMedia;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public Date getUpdatedOn() {
        return updatedOn;
    }

    public Set<ExperienceAddonMapper> getAddonMappers() {
        return addonMappers;
    }

    public void setAddonMappers(Set<ExperienceAddonMapper> addonMappers) {
        this.addonMappers = addonMappers;
    }
}
