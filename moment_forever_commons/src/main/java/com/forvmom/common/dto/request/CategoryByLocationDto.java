package com.forvmom.common.dto.request;

public class CategoryByLocationDto {
    private Long id;
    private String name;
    private String slug;
    private Integer displayOrder;

    public CategoryByLocationDto(Long id, String name, String slug, Integer displayOrder) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.displayOrder = displayOrder;
    }

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

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}