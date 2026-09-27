package com.forvmom.core.mapper;

import com.forvmom.common.dto.request.ExperienceTimeSlotAttachRequestDto;
import com.forvmom.common.dto.request.TimeSlotRequestDto;
import com.forvmom.common.dto.response.ExperienceLocationMapperDto;
import com.forvmom.common.dto.response.ExperienceLocationResponseDto;
import com.forvmom.common.dto.response.ExperienceTimeSlotResponseDto;
import com.forvmom.common.dto.response.TimeSlotResponseDto;
import com.forvmom.data.entities.ExperienceLocationMapper;
import com.forvmom.data.entities.ExperienceTimeSlotMapper;
import com.forvmom.data.entities.TimeSlot;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Stateless mapper for TimeSlot entity ↔ DTOs.
 * Follows the same pattern as SubCategoryBeanMapper / AddonBeanMapper.
 * <p>
 * Naming convention:
 * mapEntityToDto — entity → response DTO
 * mapDtoToEntity — request DTO → new entity
 * updateEntityFromDto — request DTO → existing entity (for PUT/PATCH)
 */
public class TimeSlotBeanMapper {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private TimeSlotBeanMapper() {
    }

    public static TimeSlotResponseDto mapEntityToDto(TimeSlot entity) {
        if (entity == null)
            return null;
        TimeSlotResponseDto dto = new TimeSlotResponseDto();
        dto.setId(entity.getId());
        dto.setStartTime(entity.getStartTime().format(TIME_FORMATTER));
        dto.setEndTime(entity.getEndTime().format(TIME_FORMATTER));
        dto.setIsActive(entity.getIsActive());
        dto.setLabel(entity.getLabel());
        return dto;
    }

    public static TimeSlot mapDtoToEntity(TimeSlotRequestDto dto) {
        TimeSlot entity = new TimeSlot();
        return updateEntityFromDto(entity, dto);
    }

    public static TimeSlot updateEntityFromDto(TimeSlot entity, TimeSlotRequestDto dto) {
        entity.setStartTime(parseTime(dto.getStartTime()));
        entity.setEndTime(parseTime(dto.getEndTime()));
        if (dto.getActive() != null)
            entity.setIsActive(dto.getActive());
        entity.setLabel(dto.getLabel());
        return entity;
    }

    // ── ExperienceTimeSlotMapper (junction) ───────────────────────────────────

    /**
     * Maps a junction row → response DTO.
     * Embeds TimeSlot master data with per-location configuration and pricing.
     */
    public static ExperienceTimeSlotResponseDto mapMapperEntityToDto(ExperienceTimeSlotMapper mapper) {
        if (mapper == null)
            return null;
        ExperienceTimeSlotResponseDto dto = new ExperienceTimeSlotResponseDto();
        dto.setMapperId(mapper.getId());

        TimeSlot ts = mapper.getTimeSlot();
        if (ts != null) {
            dto.setTimeSlotId(ts.getId());
            dto.setStartTime(ts.getStartTime().format(TIME_FORMATTER));
            dto.setEndTime(ts.getEndTime().format(TIME_FORMATTER));
        }

//        ExperienceLocationMapperDto experienceLocationMapperDto = ExperienceLocationBeanMapper.mapEntityToDto(mapper.getExperienceLocation());
//        dto.setExperienceLocationMapperDto(experienceLocationMapperDto);

        dto.setPriceOverride(mapper.getPriceOverride());
        dto.setMaxCapacity(mapper.getMaxCapacity());
        dto.setValidFrom(mapper.getValidFrom());
        dto.setValidTo(mapper.getValidTo());
        dto.setIsActive(mapper.getIsActive());
        return dto;
    }

    public static List<ExperienceTimeSlotResponseDto> mapMapperEntitiesToDto(List<ExperienceTimeSlotMapper> mappers) {
        if (mappers == null || mappers.isEmpty())
            return Collections.emptyList();
        return mappers.stream()
                .map(TimeSlotBeanMapper::mapMapperEntityToDto)
                .collect(Collectors.toList());
    }

    public static ExperienceTimeSlotMapper mapDtoToMapperEntity(ExperienceTimeSlotAttachRequestDto dto) {
        ExperienceTimeSlotMapper mapper = new ExperienceTimeSlotMapper();
        return updateMapperEntityFromDto(mapper, dto);
    }

    public static ExperienceTimeSlotMapper updateMapperEntityFromDto(ExperienceTimeSlotMapper mapper,
                                                                     ExperienceTimeSlotAttachRequestDto dto) {
        mapper.setPriceOverride(dto.getPriceOverride());
        mapper.setMaxCapacity(dto.getMaxCapacity());
        mapper.setValidFrom(dto.getValidFrom());
        mapper.setValidTo(dto.getValidTo());
        if (dto.getIsActive() != null)
            mapper.setIsActive(dto.getIsActive());
        return mapper;
    }

    // ── Shared util ───────────────────────────────────────────────────────────

    public static LocalTime parseTime(String time) {
        return LocalTime.parse(time, TIME_FORMATTER);
    }
}
