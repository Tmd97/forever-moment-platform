package com.forvmom.core.mapper;

import com.forvmom.common.dto.request.CategoryRequestDto;
import com.forvmom.common.dto.request.SubCategoryRequestDto;
import com.forvmom.common.dto.response.CategoryResponseDto;
import com.forvmom.common.dto.response.SubCategoryResponseDto;
import com.forvmom.data.entities.Category;
import com.forvmom.data.entities.SubCategory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Static conversions between {@link Category} entities and their request and
 * response DTOs.
 *
 * <p>
 * The read direction also maps the nested sub-categories, delegating to
 * {@link SubCategoryBeanMapper}; the write direction covers the category's own
 * fields only.
 */
public class CategoryBeanMapper {

    /**
     * Copies the writable category fields from a request DTO onto an existing
     * entity. Nested sub-categories are not touched.
     *
     * @param dto    source of the new values
     * @param entity entity to mutate in place
     */
    public static void mapDtoToEntity(CategoryRequestDto dto, Category entity) {
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setSlug(dto.getSlug());
        entity.setDisplayOrder(dto.getDisplayOrder());
        entity.setActive(dto.getIsActive());
    }
    // public static void mapDtoToEntity(CategoryRequestDto dto, Category entity) {
    // entity.setName(dto.getName());
    // entity.setDescription(dto.getDescription());
    // entity.setSlug(dto.getSlug());
    // entity.setDisplayOrder(dto.getDisplayOrder());
    //
    // // Handle SubCategories mapping
    // if (dto.getSubCategories() != null && !dto.getSubCategories().isEmpty()) {
    // for (SubCategoryRequestDto subCatDto : dto.getSubCategories()) {
    // SubCategory subCategory = new SubCategory();
    // SubCategoryBeanMapper.mapDtoToEntity(subCatDto, subCategory);
    // entity.setSubCategory(subCategory);
    // }
    // }
    // }

    /**
     * Maps a category entity to its response DTO, including its sub-categories when
     * the collection is loaded and non-empty.
     *
     * @param entity the category entity to convert
     * @return the populated {@link CategoryResponseDto}
     */
    public static CategoryResponseDto mapEntityToDto(Category entity) {
        CategoryResponseDto dto = new CategoryResponseDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setSlug(entity.getSlug());
        dto.setDisplayOrder(entity.getDisplayOrder());
        dto.setIsActive(entity.isActive());

        // Map SubCategories
        if (entity.getSubCategories() == null || entity.getSubCategories().isEmpty()) {
            return dto;
        }
        List<SubCategoryResponseDto> subCatDtos = entity.getSubCategories().stream()
                .map(SubCategoryBeanMapper::mapEntityToDto)
                .collect(Collectors.toList());
        dto.setSubCategories(subCatDtos);

        return dto;
    }
}
