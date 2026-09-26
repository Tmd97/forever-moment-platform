package com.forvmom.core.mapper;

import com.forvmom.common.dto.request.AddonRequestDto;
import com.forvmom.common.dto.response.AddonResponseDto;
import com.forvmom.common.dto.response.ExperienceAddonResponseDto;
import com.forvmom.data.entities.Addon;
import com.forvmom.data.entities.ExperienceAddonMapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AddonBeanMapper {

    private AddonBeanMapper() {
    }

    // ── Master Addon mapping ──────────────────────────────────────────────────

    /**
     * Maps a master {@link Addon} entity to a response DTO.
     * {@code mapperId} is NOT set here — only populated in the experience-scoped
     * context.
     */
    public static AddonResponseDto mapAddonToDto(Addon addon) {
        if (addon == null)
            return null;
        AddonResponseDto dto = new AddonResponseDto();
        dto.setId(addon.getId());
        dto.setName(addon.getName());
        dto.setDescription(addon.getDescription());
        dto.setIcon(addon.getIcon());
        if (addon.getImageMedia() != null) {
            dto.setMediaId(addon.getImageMedia().getId());
        }
        dto.setBasePrice(addon.getBasePrice());
        dto.setEffectivePrice(addon.getBasePrice()); // no override at this level
        dto.setIsFree(false);
        dto.setIsActive(addon.getIsActive());
        return dto;
    }

    // ── Junction mapper → ExperienceAddonResponseDto ─────────────────────────

    /**
     * Maps an {@link ExperienceAddonMapper} junction row to a flat response DTO.
     *
     * <p>
     * The returned {@code mapperId} is the {@code ExperienceAddonMapper.id}
     * and should be used as the {@code addonMapperIds} value in booking requests.
     */
    public static ExperienceAddonResponseDto mapAddonMapperToDto(ExperienceAddonMapper mapper) {
        if (mapper == null || mapper.getAddon() == null)
            return null;
        Addon addon = mapper.getAddon();

        ExperienceAddonResponseDto dto = new ExperienceAddonResponseDto();
        dto.setMapperId(mapper.getId()); // ExperienceAddonMapper.id (for booking)
        dto.setAddonId(addon.getId()); // master Addon.id
        dto.setName(addon.getName());
        dto.setDescription(addon.getDescription());
        dto.setIcon(addon.getIcon());
        if (addon.getImageMedia() != null) {
            dto.setMediaId(addon.getImageMedia().getId());
        }
        dto.setBasePrice(addon.getBasePrice());
        dto.setPriceOverride(mapper.getPriceOverride());
        dto.setEffectivePrice(mapper.effectivePrice()); // resolved: free→0, override→override, else base
        dto.setIsFree(Boolean.TRUE.equals(mapper.getIsFree()));
        dto.setIsActive(mapper.getIsActive());
        return dto;
    }

    /**
     * Maps a list of junction mapper rows to DTOs.
     */
    public static List<ExperienceAddonResponseDto> mapAddonMappers(List<ExperienceAddonMapper> mappers) {
        if (mappers == null || mappers.isEmpty())
            return Collections.emptyList();
        return mappers.stream()
                .map(AddonBeanMapper::mapAddonMapperToDto)
                .collect(Collectors.toList());
    }

    // ── Request DTO → Entity ──────────────────────────────────────────────────

    /**
     * Maps an {@link AddonRequestDto} to a new {@link Addon} entity.
     */
    public static Addon mapRequestToAddon(AddonRequestDto dto) {
        return applyRequest(dto, new Addon());
    }

    /**
     * Applies fields from an {@link AddonRequestDto} onto an existing
     * {@link Addon}.
     */
    public static Addon updateAddonFromRequest(Addon addon, AddonRequestDto dto) {
        return applyRequest(dto, addon);
    }

    private static Addon applyRequest(AddonRequestDto dto, Addon addon) {
        addon.setName(dto.getName());
        addon.setDescription(dto.getDescription());
        addon.setIcon(dto.getIcon());
        addon.setBasePrice(dto.getBasePrice());
        if (dto.getIsActive() != null) {
            addon.setIsActive(dto.getIsActive());
        }
        return addon;
    }
}
