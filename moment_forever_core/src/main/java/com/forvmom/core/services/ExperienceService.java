package com.forvmom.core.services;

import com.forvmom.common.dto.request.ExperienceCreateRequestDto;
import com.forvmom.common.dto.request.ExperienceDetailRequestDto;
import com.forvmom.common.dto.response.ExperienceDetailResponseDto;
import com.forvmom.common.dto.response.ExperienceHighlightResponseDto;
import com.forvmom.common.dto.response.ExperienceResponseDto;

import java.util.List;

public interface ExperienceService {

    /**
     * Creates both Experience and ExperienceDetail rows from one combined request
     */
    ExperienceResponseDto createExperience(ExperienceCreateRequestDto requestDto);

    /**
     * Updates both Experience and ExperienceDetail rows from one combined request
     */
    ExperienceResponseDto updateExperience(Long id, ExperienceCreateRequestDto requestDto);

    ExperienceResponseDto getById(Long id);

    ExperienceResponseDto getBySlug(String slug);

    /**
     * Returns lightweight list of all experiences (basic + highlight fields, no
     * full detail)
     */
    List<ExperienceHighlightResponseDto> getAll();

    /** Returns lightweight list of active experiences only */
    List<ExperienceHighlightResponseDto> getAllActive();

    /**
     * Returns lightweight list of active experiences, optionally filtered to
     * only those serviceable at the given pincode.
     *
     * @param pincodeCode the pincode to filter by, or {@code null}/blank for no
     *                     filtering
     */
    List<ExperienceHighlightResponseDto> getAllActive(String pincodeCode);

    /** Returns lightweight list filtered by sub-category */
    List<ExperienceHighlightResponseDto> getBySubCategory(Long subCategoryId);

    /**
     * Returns lightweight list filtered by sub-category, optionally further
     * filtered to only those serviceable at the given pincode.
     *
     * @param pincodeCode the pincode to filter by, or {@code null}/blank for no
     *                     filtering
     */
    List<ExperienceHighlightResponseDto> getBySubCategory(Long subCategoryId, String pincodeCode);

    /** Returns lightweight list of featured active experiences */
    List<ExperienceHighlightResponseDto> getFeatured();

    /**
     * Returns lightweight list of featured active experiences, optionally
     * filtered to only those serviceable at the given pincode.
     *
     * @param pincodeCode the pincode to filter by, or {@code null}/blank for no
     *                     filtering
     */
    List<ExperienceHighlightResponseDto> getFeatured(String pincodeCode);

    boolean deleteExperience(Long id);

    void toggleActive(Long id);

    void toggleFeatured(Long id);

    // ExperienceDetail operations (upsert — create or update)
    ExperienceDetailResponseDto upsertDetail(Long experienceId, ExperienceDetailRequestDto requestDto);

    ExperienceDetailResponseDto getDetail(Long experienceId);

    /**
     * Checks whether an experience is serviceable at a specific pincode.
     *
     * <p>
     * Resolves the pincode's owning location, finds the experience's active
     * attachment to that location, and returns {@code true} when either that
     * attachment has no pincode restriction (serviceable everywhere in the
     * location) or it explicitly whitelists this pincode.
     *
     * @param experienceId the experience identifier
     * @param pincodeCode  the pincode code entered by the customer
     * @return {@code true} when the experience is serviceable at the pincode
     * @throws com.forvmom.common.errorhandler.ResourceNotFoundException if the
     *         experience or pincode is unknown
     */
    boolean isExperienceServiceableAtPincode(Long experienceId, String pincodeCode);
}
