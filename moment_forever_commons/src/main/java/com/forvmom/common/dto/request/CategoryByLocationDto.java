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
    // getters
}