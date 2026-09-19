package com.forvmom.core.services;

import com.forvmom.common.dto.request.*;
import com.forvmom.common.dto.response.*;

import java.util.List;

/**
 * Contract for managing service {@code Location}s, their serviceable pincodes,
 * and the mappings that connect locations to experiences, categories and
 * sub-categories.
 *
 * <p>
 * A location is the geographic unit the catalog is sliced by: customers pick a
 * location (or a pincode resolves to one) and the catalog surface for that
 * location is driven by the category, sub-category and experience attachments
 * defined here. Attachments can carry their own display order, active flag and,
 * for experiences, a price override and validity window.
 *
 * <p>
 * Implementations are expected to run each operation transactionally and to
 * keep the Redis catalog snapshots consistent when experience attachments
 * change.
 */
public interface LocationService {

    /**
     * Creates a new location.
     *
     * @param requestDto the location attributes; the name must be unique
     * @return the created location
     */
    LocationResponseDto createLocation(LocationRequestDto requestDto);

    /**
     * Updates an existing location.
     *
     * @param id         the location identifier
     * @param requestDto the new location attributes
     * @return the updated location
     */
    LocationResponseDto updateLocation(Long id, LocationRequestDto requestDto);

    /**
     * Loads a single location, including its pincodes.
     *
     * @param id the location identifier
     * @return the matching location
     */
    LocationResponseDto getById(Long id);

    /**
     * Lists every location regardless of active state.
     *
     * @return all locations, or an empty list when none exist
     */
    List<LocationResponseDto> getAll();

    /**
     * Lists only the locations currently flagged active.
     *
     * @return the active locations, or an empty list when none exist
     */
    List<LocationResponseDto> getAllActive();

    /**
     * Lists the locations belonging to a city.
     *
     * @param city the city name to filter on
     * @return the matching locations
     */
    List<LocationResponseDto> getByCity(String city);

    /**
     * Deletes a location.
     *
     * @param id the location identifier
     * @return {@code true} when the delete was issued
     */
    boolean deleteLocation(Long id);

    /**
     * Flips the active flag of a location.
     *
     * @param id the location identifier
     */
    void toggleActive(Long id);

    // Pincode operations

    /**
     * Adds a serviceable pincode to a location.
     *
     * @param requestDto the pincode attributes, including the owning location id
     * @return the created pincode
     */
    PincodeResponseDto addPincode(PincodeRequestDto requestDto);

    /**
     * Updates an existing pincode.
     *
     * @param pincodeId  the pincode identifier
     * @param requestDto the new pincode attributes
     * @return the updated pincode
     */
    PincodeResponseDto updatePincode(Long pincodeId, PincodeRequestDto requestDto);

    /**
     * Lists the pincodes registered for a location.
     *
     * @param locationId the location identifier
     * @return the pincodes of that location
     */
    List<PincodeResponseDto> getPincodesByLocation(Long locationId);

    /**
     * Serviceability check for a raw pincode code.
     *
     * @param pincodeCode the pincode code entered by the customer
     * @return the matching pincode when it is serviceable
     */
    PincodeResponseDto checkPincode(String pincodeCode);

    /**
     * Deletes a pincode.
     *
     * @param pincodeId the pincode identifier
     * @return {@code true} when the delete was issued
     */
    boolean deletePincode(Long pincodeId);

    // ── Experience Association ────────────────────────────────────────────────

    /**
     * Attaches a location to an experience, optionally overriding price and
     * setting a validity window. Implementations warm the catalog cache so the
     * booking enrichment path sees the new mapping.
     *
     * @param locationId   the location identifier
     * @param experienceId the experience identifier
     * @param requestDto   price override, validity window and active flag
     * @return the created experience-location mapping
     */
    ExperienceLocationResponseDto attachToExperience(Long locationId, Long experienceId,
            ExperienceLocationAttachRequestDto requestDto);

    /**
     * Removes the mapping between a location and an experience and evicts the
     * corresponding catalog cache entry.
     *
     * @param locationId   the location identifier
     * @param experienceId the experience identifier
     */
    void detachFromExperience(Long locationId, Long experienceId);

    /**
     * Lists the experience mappings defined for a location.
     *
     * @param locationId the location identifier
     * @return the experience-location mappings for that location
     */
    List<ExperienceLocationResponseDto> getExperiencesForLocation(Long locationId);

    /**
     * Updates an existing experience-location mapping and re-warms its cache
     * entry.
     *
     * @param locationId   the location identifier
     * @param experienceId the experience identifier
     * @param requestDto   the new price override, validity window and active flag
     * @return the updated mapping
     */
    ExperienceLocationResponseDto updateExperienceAttachment(Long locationId, Long experienceId,
            ExperienceLocationAttachRequestDto requestDto);

    /**
     * Flips the active flag of an experience-location mapping and re-warms its
     * cache entry.
     *
     * @param mapperId the experience-location mapping identifier
     */
    void toggleExperienceAttachmentActive(Long mapperId);

    /// /////////category association with location
    /**
     * Attaches a category to a location so it appears in that location's catalog.
     *
     * @param locationId the location identifier
     * @param categoryId the category identifier
     * @param requestDto display order and active flag for the mapping
     * @return the created category-location mapping
     */
    CategoryLocationResponseDto attachCategoryToLocation(Long locationId, Long categoryId, CategoryLocationAttachRequestDto requestDto);

    /**
     * Removes a category from a location's catalog.
     *
     * @param locationId the location identifier
     * @param categoryId the category identifier
     */
    void detachCategoryFromLocation(Long locationId, Long categoryId);

    /**
     * Lists all category mappings of a location, active or not.
     *
     * @param locationId the location identifier
     * @return the category-location mappings
     */
    List<CategoryLocationResponseDto> getCategoriesForLocation(Long locationId);

    /**
     * Updates the display order and/or active flag of a category-location mapping.
     *
     * @param locationId the location identifier
     * @param categoryId the category identifier
     * @param requestDto the fields to update; {@code null} fields are left as-is
     * @return the updated mapping
     */
    CategoryLocationResponseDto updateCategoryAttachment(Long locationId, Long categoryId, CategoryLocationAttachRequestDto requestDto);

    /**
     * Flips the active flag of a category-location mapping.
     *
     * @param mapperId the category-location mapping identifier
     */
    void toggleCategoryAttachmentActive(Long mapperId);

    /**
     * Public catalog query: the active categories offered at a location, in a
     * lightweight shape carrying id, name, slug and display order.
     *
     * @param locationId the location identifier
     * @return the active categories for that location
     */
    List<CategoryByLocationDto> getActiveCategoriesByLocation(Long locationId); // public

    /// //////////sub-category association with location
    /**
     * Attaches a sub-category to a location so it appears in that location's
     * catalog.
     *
     * @param locationId    the location identifier
     * @param subCategoryId the sub-category identifier
     * @param requestDto    display order and active flag for the mapping
     * @return the created sub-category-location mapping
     */
    SubCategoryLocationResponseDto attachSubCategoryToLocation(Long locationId, Long subCategoryId, SubCategoryLocationAttachRequestDto requestDto);

    /**
     * Removes a sub-category from a location's catalog.
     *
     * @param locationId    the location identifier
     * @param subCategoryId the sub-category identifier
     */
    void detachSubCategoryFromLocation(Long locationId, Long subCategoryId);

    /**
     * Lists all sub-category mappings of a location, active or not.
     *
     * @param locationId the location identifier
     * @return the sub-category-location mappings
     */
    List<SubCategoryLocationResponseDto> getSubCategoriesForLocation(Long locationId);

    /**
     * Updates the display order and/or active flag of a sub-category-location
     * mapping.
     *
     * @param locationId    the location identifier
     * @param subCategoryId the sub-category identifier
     * @param requestDto    the fields to update; {@code null} fields are left as-is
     * @return the updated mapping
     */
    SubCategoryLocationResponseDto updateSubCategoryAttachment(Long locationId, Long subCategoryId, SubCategoryLocationAttachRequestDto requestDto);

    /**
     * Flips the active flag of a sub-category-location mapping.
     *
     * @param mapperId the sub-category-location mapping identifier
     */
    void toggleSubCategoryAttachmentActive(Long mapperId);

    /**
     * Public catalog query: the active sub-categories offered at a location,
     * including their parent category id and name.
     *
     * @param locationId the location identifier
     * @return the active sub-categories for that location
     */
    List<SubCategoryByLocationDto> getActiveSubCategoriesByLocation(Long locationId);

    /**
     * Public catalog query: the active sub-categories offered at a location that
     * belong to a specific parent category.
     *
     * @param locationId the location identifier
     * @param categoryId the parent category identifier
     * @return the matching active sub-categories
     */
    List<SubCategoryByLocationDto> getActiveSubCategoriesByLocationAndCategory(Long locationId, Long categoryId);
}
