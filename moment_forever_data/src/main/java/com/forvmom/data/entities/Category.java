package com.forvmom.data.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "category")
@SQLDelete(sql = "UPDATE category SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
public class Category extends NamedEntity {

    @Column(name = "description")
    private String description;

    @Column(name = "display_order")
    private Long displayOrder;

    @Column(name = "slug")
    private String slug;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<SubCategory> subCategories = new ArrayList<>();

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<CategoryMediaMapper> mediaMappers = new ArrayList<>();

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Long displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setSubCategory(SubCategory subCategory) {
        subCategories.add(subCategory);
        subCategory.setCategory(this);
    }

    public void removeSubCategory(SubCategory subCategory) {
        subCategories.remove(subCategory);
        subCategory.setCategory(null);
    }

    public List<SubCategory> getSubCategories() {
        return subCategories;
    }

    public void clearSubCategories() {
        for (SubCategory subCategory : subCategories) {
            subCategory.setCategory(null);
        }
        subCategories.clear();
    }

    public List<CategoryMediaMapper> getMediaMappers() {
        return mediaMappers;
    }

    public void addMediaMapper(CategoryMediaMapper mediaMapper) {
        mediaMappers.add(mediaMapper);
        mediaMapper.setCategory(this);
    }

    public void removeMediaMapper(CategoryMediaMapper mediaMapper) {
        mediaMappers.remove(mediaMapper);
        mediaMapper.setCategory(null);
    }

}