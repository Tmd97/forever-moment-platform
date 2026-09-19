package com.forvmom.core.mapper;

import com.forvmom.common.dto.response.ExperienceLocationMapperDto;
import com.forvmom.common.dto.response.ExperienceLocationResponseDto;
import com.forvmom.common.dto.response.ExperienceMediaResponseDto;
import com.forvmom.core.config.ImageUrlConfig;
import com.forvmom.data.entities.ExperienceLocationMapper;
import com.forvmom.data.entities.ExperienceMediaMapper;

/**
 * Static conversion from the {@link ExperienceLocationMapper} junction entity to
 * its flat DTO.
 *
 * <p>
 * The junction row carries the experience/location pair plus the pricing and
 * validity overrides that apply when that experience is offered at that
 * location; only the two ids are exposed, not the full related objects.
 */
public class ExperienceLocationBeanMapper {
    /**
     * Flattens a junction row into its DTO, replacing the related experience and
     * location with their ids.
     *
     * @param entity the junction entity, may be {@code null}
     * @return the DTO, or {@code null} if {@code entity} is {@code null}
     */
    public static ExperienceLocationMapperDto mapEntityToDto(ExperienceLocationMapper entity) {
        if (entity == null) {
            return null;
        }
        ExperienceLocationMapperDto dto = new ExperienceLocationMapperDto();
        dto.setId(entity.getId());
        dto.setExperienceId(entity.getExperience().getId());
        dto.setLocationId(entity.getLocation().getId());
        dto.setPriceOverride(entity.getPriceOverride());
        dto.setIsActive(entity.getIsActive());
        dto.setValidFrom(entity.getValidFrom());
        dto.setValidTo(entity.getValidTo());
        return dto;
    }
}
