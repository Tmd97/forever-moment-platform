package com.forvmom.core.controller.admin;

import com.forvmom.common.dto.request.ExperienceCreateRequestDto;
import com.forvmom.common.dto.request.ExperienceDetailRequestDto;
import com.forvmom.common.dto.request.ReorderRequestDto;
import com.forvmom.common.dto.response.ExperienceDetailResponseDto;
import com.forvmom.common.dto.response.ExperienceHighlightResponseDto;
import com.forvmom.common.dto.response.ExperienceResponseDto;
import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.common.utils.AppConstants;
import com.forvmom.core.services.ExperienceService;
import com.forvmom.core.services.ReorderingService;
import com.forvmom.data.entities.Category;
import com.forvmom.data.entities.Experience;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin CRUD endpoints for experiences.
 *
 * <p>
 * Mapped under {@code /admin/experiences} and intended for administrators only.
 * Create and update accept a single {@link ExperienceCreateRequestDto} that
 * covers both the {@code Experience} and its {@code ExperienceDetail}. Unlike
 * the public controller, the list endpoint is not restricted to active rows;
 * deletion is soft, and active/featured flags plus display order can be
 * toggled here.
 */
@RestController
@RequestMapping("/admin/experiences")
@Tag(name = "Admin Experience API", description = "Endpoints for managing experiences (Admin only)")
public class ExperienceControllerAdmin {

    @Autowired
    private ReorderingService reorderingService;

    @Autowired
    private ExperienceService experienceService;

    /**
     * Creates an experience together with its detail section.
     *
     * @param requestDto the experience and detail values to persist
     * @return {@code 201 CREATED} wrapping the created
     *         {@link ExperienceResponseDto}
     */
    @PostMapping
    @Operation(summary = "Create Experience", description = "Creates both Experience and ExperienceDetail in one request")
    public ResponseEntity<ApiResponse<?>> create(@RequestBody ExperienceCreateRequestDto requestDto) {
        ExperienceResponseDto response = experienceService.createExperience(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildCreatedResponse(response, AppConstants.MSG_CREATED));
    }

    //TODO: since we need to show as highlight we need to fetch for experience basic details only
    /**
     * Lists all experiences as lightweight highlight cards.
     *
     * @return {@code 200 OK} wrapping the list of
     *         {@link ExperienceHighlightResponseDto}
     */
    @GetMapping
    @Operation(summary = "Get All Experiences")
    public ResponseEntity<ApiResponse<?>> getAll() {
        List<ExperienceHighlightResponseDto> response = experienceService.getAll();
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Returns one experience with its full detail section.
     *
     * @param id identifier of the experience
     * @return {@code 200 OK} wrapping the {@link ExperienceResponseDto}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get Experience by ID (with detail)")
    public ResponseEntity<ApiResponse<?>> getById(@PathVariable Long id) {
        ExperienceResponseDto response = experienceService.getById(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Returns one experience with its full detail section, addressed by slug.
     *
     * @param slug unique slug of the experience
     * @return {@code 200 OK} wrapping the {@link ExperienceResponseDto}
     */
    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get Experience by Slug (with detail)")
    public ResponseEntity<ApiResponse<?>> getBySlug(@PathVariable String slug) {
        ExperienceResponseDto response = experienceService.getBySlug(slug);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Lists the experiences belonging to a sub-category, as highlight cards.
     *
     * @param subCategoryId identifier of the owning sub-category
     * @return {@code 200 OK} wrapping the list of
     *         {@link ExperienceHighlightResponseDto}
     */
    @GetMapping("/subcategory/{subCategoryId}")
    @Operation(summary = "Get Experiences by SubCategory")
    public ResponseEntity<ApiResponse<?>> getBySubCategory(@PathVariable Long subCategoryId) {
        List<ExperienceHighlightResponseDto> response = experienceService.getBySubCategory(subCategoryId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Updates an experience and its detail section in one request.
     *
     * @param id         identifier of the experience to update
     * @param requestDto the new experience and detail values
     * @return {@code 200 OK} wrapping the updated {@link ExperienceResponseDto}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update Experience", description = "Updates both Experience and ExperienceDetail in one request")
    public ResponseEntity<ApiResponse<?>> update(@PathVariable Long id,
            @RequestBody ExperienceCreateRequestDto requestDto) {
        ExperienceResponseDto response = experienceService.updateExperience(id, requestDto);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_UPDATED));
    }

    /**
     * Soft-deletes an experience so it disappears from the catalog without being
     * removed from the database.
     *
     * @param id identifier of the experience to delete
     * @return {@code 200 OK} with an empty payload
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Experience (soft)")
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable Long id) {
        experienceService.deleteExperience(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, AppConstants.MSG_DELETED));
    }

    /**
     * Flips the active flag of an experience, controlling whether the public
     * catalog lists it.
     *
     * @param id identifier of the experience
     * @return {@code 200 OK} with an empty payload
     */
    @PatchMapping("/{id}/toggle-active")
    @Operation(summary = "Toggle Experience Active Status")
    public ResponseEntity<ApiResponse<?>> toggleActive(@PathVariable Long id) {
        experienceService.toggleActive(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, "Experience active status toggled"));
    }

    /**
     * Flips the featured flag of an experience, controlling whether it appears in
     * the featured/homepage listing.
     *
     * @param id identifier of the experience
     * @return {@code 200 OK} with an empty payload
     */
    @PatchMapping("/{id}/toggle-featured")
    @Operation(summary = "Toggle Experience Featured Status")
    public ResponseEntity<ApiResponse<?>> toggleFeatured(@PathVariable Long id) {
        experienceService.toggleFeatured(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, "Experience featured status toggled"));
    }

    /**
     * Moves an experience to a new 1-based display position, shifting the
     * experiences it passes over so positions stay contiguous.
     *
     * @param reorderRequestDto carries the experience id and its target position
     * @return {@code 200 OK} with an empty payload
     */
    @PatchMapping("/reorder")
    public ResponseEntity<ApiResponse<?>> reOrderTheItems(
            @RequestBody ReorderRequestDto reorderRequestDto) {
        reorderingService.reorderItems(reorderRequestDto.getId(), reorderRequestDto.getNewPosition(), Experience.class);
        return ResponseEntity.ok(
                ResponseUtil.buildOkResponse(null, AppConstants.MSG_UPDATED));
    }
}
