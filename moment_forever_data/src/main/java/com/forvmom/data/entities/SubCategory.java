package com.forvmom.data.entities;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sub_category")
@org.hibernate.annotations.SQLDelete(sql = "UPDATE sub_category SET deleted = true WHERE id = ?")
@org.hibernate.annotations.Where(clause = "deleted = false")
public class SubCategory extends NamedEntity {

    @Column(name = "description")
    private String description;

    @Column(name = "display_order")
    private Long displayOrder;

    @Column(name = "slug")
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "subCategory", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<SubCategoryMediaMapper> mediaMappers = new ArrayList<>();

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

    public void setCategory(Category category) {
        this.category = category;
    }

    public Category getCategory() {
        return category;
    }

    public List<SubCategoryMediaMapper> getMediaMappers() {
        return mediaMappers;
    }

    public void addMediaMapper(SubCategoryMediaMapper mediaMapper) {
        mediaMappers.add(mediaMapper);
        mediaMapper.setSubCategory(this);
    }

    public void removeMediaMapper(SubCategoryMediaMapper mediaMapper) {
        mediaMappers.remove(mediaMapper);
        mediaMapper.setSubCategory(null);
    }
}
