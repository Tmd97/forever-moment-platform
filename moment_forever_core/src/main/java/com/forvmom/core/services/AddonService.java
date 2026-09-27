package com.forvmom.core.services;

import com.forvmom.common.dto.request.AddonRequestDto;
import com.forvmom.common.dto.request.BulkAttachAddonRequestDto;
import com.forvmom.common.dto.response.AddonResponseDto;
import com.forvmom.common.dto.response.BulkAttachAddonResultDto;
import com.forvmom.common.dto.response.ExperienceAddonResponseDto;

import java.math.BigDecimal;
import java.util.List;

public interface AddonService {

    /** Create a new master addon record */
    AddonResponseDto createAddon(AddonRequestDto requestDto);

    /** Get all master addon records */
    List<AddonResponseDto> getAllAddons();

    /** Update a master addon record */
    AddonResponseDto updateAddon(Long id, AddonRequestDto requestDto);

    /** Attach/update image media for a master addon record */
    AddonResponseDto updateAddonImage(Long id, Long mediaId);

    /**
     * Soft-delete a master addon record.
     * Also soft-deletes all junction rows to remove it from all experiences.
     */
    boolean deleteAddon(Long id);

    /**
     * Attach a single master addon to an experience.
     *
     * @param priceOverride null = use Addon.basePrice
     * @param isFree        true = complimentary regardless of price
     * @return the created ExperienceAddonResponseDto (with mapperId)
     */
    ExperienceAddonResponseDto attachToExperience(Long experienceId, Long addonId,
            BigDecimal priceOverride, Boolean isFree);

    /**
     * Bulk-attach multiple addons to an experience in one transaction.
     * Skips (does not fail) addons that are already attached or not found.
     *
     * @return result with lists of attached and skipped items
     */
    BulkAttachAddonResultDto attachAddons(Long experienceId, BulkAttachAddonRequestDto requestDto);

    /**
     * Detach an addon from an experience (junction row soft-deleted; master stays).
     */
    void detachFromExperience(Long experienceId, Long addonId);

    /** List all addons attached to a specific experience with effective pricing */
    List<ExperienceAddonResponseDto> getAddonsForExperience(Long experienceId);
}
