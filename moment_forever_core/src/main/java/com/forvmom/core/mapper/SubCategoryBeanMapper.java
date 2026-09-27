package com.forvmom.core.mapper;

import com.forvmom.common.dto.request.SubCategoryRequestDto;
import com.forvmom.common.dto.response.SubCategoryResponseDto;
import com.forvmom.data.entities.SubCategory;

/**
 * Static conversions between {@link SubCategory} entities and their request and
 * response DTOs.
 */
public class SubCategoryBeanMapper {

    /**
     * Copies the writable sub-category fields from a request DTO onto an existing
     * entity. Does nothing when either argument is {@code null}, and the parent
     * category association is left unchanged.
     *
     * @param dto    source of the new values
     * @param entity entity to mutate in place
     */
    public static void mapDtoToEntity(SubCategoryRequestDto dto, SubCategory entity) {
        if (dto == null || entity == null) return;

        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setSlug(dto.getSlug());
        entity.setDisplayOrder(dto.getDisplayOrder());
        entity.setActive(dto.getIsActive());

    }

    /**
     * Maps a sub-category entity to its response DTO, denormalising the parent
     * category's id, name and slug onto the result when a parent is present.
     *
     * @param entity the sub-category entity, may be {@code null}
     * @return the DTO, or {@code null} if {@code entity} is {@code null}
     */
    public static SubCategoryResponseDto mapEntityToDto(SubCategory entity) {
        if (entity == null) return null;

        SubCategoryResponseDto dto = new SubCategoryResponseDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCategoryId(entity.getCategory().getId());
        dto.setDescription(entity.getDescription());
        dto.setSlug(entity.getSlug());
        dto.setDisplayOrder(entity.getDisplayOrder());
        dto.setIsActive(entity.isActive());

        // Set category info if category exists

        if (entity.getCategory() == null) {
            return dto;
        }
        dto.setCategoryId(entity.getCategory().getId());
        dto.setCategoryName(entity.getCategory().getName());
        dto.setCategorySlug(entity.getCategory().getSlug());

        return dto;
    }
}