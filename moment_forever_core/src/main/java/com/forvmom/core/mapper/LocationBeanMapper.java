package com.forvmom.core.mapper;

import com.forvmom.common.dto.request.LocationRequestDto;
import com.forvmom.common.dto.response.CategoryLocationResponseDto;
import com.forvmom.common.dto.response.LocationResponseDto;
import com.forvmom.common.dto.response.PincodeResponseDto;
import com.forvmom.common.dto.response.SubCategoryLocationResponseDto;
import com.forvmom.data.entities.CategoryLocationMapper;
import com.forvmom.data.entities.Location;
import com.forvmom.data.entities.Pincode;
import com.forvmom.data.entities.SubCategoryLocationMapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Static conversions between {@link Location}-related entities and their DTOs.
 *
 * <p>
 * Covers the location itself, its {@link Pincode} children, and the
 * category/sub-category junction rows that decide which catalog is offered at a
 * location. Junction mappings denormalise the related names and slugs so
 * callers do not need a second lookup.
 */
public class LocationBeanMapper {

    /**
     * Copies the writable location fields from a request DTO onto an existing
     * entity. Does nothing when either argument is {@code null}.
     *
     * @param dto    source of the new values; a missing country defaults to
     *               {@code "India"} and a {@code null} active flag leaves the
     *               current value untouched
     * @param entity entity to mutate in place
     */
    public static void mapDtoToEntity(LocationRequestDto dto, Location entity) {
        if (dto == null || entity == null)
            return;

        entity.setName(dto.getName());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setCountry(dto.getCountry() != null ? dto.getCountry() : "India");
        entity.setAddress(dto.getAddress());
        entity.setLatitude(dto.getLatitude());
        entity.setLongitude(dto.getLongitude());
        if (dto.getIsActive() != null) {
            entity.setActive(dto.getIsActive());
        }
    }

    /**
     * Maps a location entity to its response DTO, embedding its pincodes when the
     * collection is loaded and non-empty. The embedded pincodes are mapped
     * shallowly, since the location is already the enclosing context.
     *
     * @param entity the location entity, may be {@code null}
     * @return the DTO, or {@code null} if {@code entity} is {@code null}
     */
    public static LocationResponseDto mapEntityToDto(Location entity) {
        if (entity == null)
            return null;

        LocationResponseDto dto = new LocationResponseDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCity(entity.getCity());
        dto.setState(entity.getState());
        dto.setCountry(entity.getCountry());
        dto.setAddress(entity.getAddress());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setIsActive(entity.isActive());
        dto.setCreatedOn(entity.getCreatedOn());
        dto.setUpdatedOn(entity.getUpdatedOn());

        if (entity.getPincodes() != null && !entity.getPincodes().isEmpty()) {
            List<PincodeResponseDto> pincodeDtos = entity.getPincodes().stream()
                    .map(p -> mapPincodeToDto(p, false))
                    .collect(Collectors.toList());
            dto.setPincodes(pincodeDtos);
        }

        return dto;
    }

    /**
     * Maps a pincode entity to its DTO, optionally denormalising its owning
     * location's id, name and city.
     *
     * @param pincode         the pincode entity, may be {@code null}
     * @param includeLocation {@code true} to copy the owning location's summary
     *                        fields; pass {@code false} when the pincode is already
     *                        nested inside that location's response
     * @return the DTO, or {@code null} if {@code pincode} is {@code null}
     */
    public static PincodeResponseDto mapPincodeToDto(Pincode pincode, boolean includeLocation) {
        if (pincode == null)
            return null;

        PincodeResponseDto dto = new PincodeResponseDto();
        dto.setId(pincode.getId());
        dto.setName(pincode.getName());
        dto.setPincodeCode(pincode.getPincodeCode());
        dto.setAreaName(pincode.getAreaName());
        dto.setLatitude(pincode.getLatitude());
        dto.setLongitude(pincode.getLongitude());
        dto.setIsActive(pincode.isActive());
        dto.setCreatedOn(pincode.getCreatedOn());

        if (includeLocation && pincode.getLocation() != null) {
            dto.setLocationId(pincode.getLocation().getId());
            dto.setLocationName(pincode.getLocation().getName());
            dto.setLocationCity(pincode.getLocation().getCity());
        }

        return dto;
    }

    /**
     * Flattens a category-to-location junction row, carrying the category's name
     * and slug and the location's name alongside their ids.
     *
     * @param entity the junction entity
     * @return the populated {@link CategoryLocationResponseDto}
     */
    public static CategoryLocationResponseDto mapCategoryLocationToDto(CategoryLocationMapper entity) {
        CategoryLocationResponseDto dto = new CategoryLocationResponseDto();
        dto.setId(entity.getId());
        dto.setCategoryId(entity.getCategory().getId());
        dto.setCategoryName(entity.getCategory().getName());
        dto.setCategorySlug(entity.getCategory().getSlug());
        dto.setLocationId(entity.getLocation().getId());
        dto.setLocationName(entity.getLocation().getName());
        dto.setDisplayOrder(entity.getDisplayOrder());
        dto.setActive(entity.getActive());
        return dto;
    }

    /**
     * Flattens a sub-category-to-location junction row. The sub-category's parent
     * category is resolved through the sub-category, so the result also carries the
     * owning category's id and name.
     *
     * @param entity the junction entity
     * @return the populated {@link SubCategoryLocationResponseDto}
     */
    public static SubCategoryLocationResponseDto mapSubCategoryLocationToDto(SubCategoryLocationMapper entity) {
        SubCategoryLocationResponseDto dto = new SubCategoryLocationResponseDto();
        dto.setId(entity.getId());
        dto.setSubCategoryId(entity.getSubCategory().getId());
        dto.setSubCategoryName(entity.getSubCategory().getName());
        dto.setSubCategorySlug(entity.getSubCategory().getSlug());
        dto.setCategoryId(entity.getSubCategory().getCategory().getId());
        dto.setCategoryName(entity.getSubCategory().getCategory().getName());
        dto.setLocationId(entity.getLocation().getId());
        dto.setLocationName(entity.getLocation().getName());
        dto.setDisplayOrder(entity.getDisplayOrder());
        dto.setActive(entity.getActive());
        return dto;
    }
}
