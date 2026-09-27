package com.forvmom.core.controller.pub;

import com.forvmom.common.dto.response.ExperienceHighlightResponseDto;
import com.forvmom.common.dto.response.ExperienceResponseDto;
import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.common.utils.AppConstants;
import com.forvmom.core.services.ExperienceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Read-only catalog browsing endpoints for experiences (the bookable decoration
 * packages).
 *
 * <p>
 * Mapped under {@code /public/experiences} and reachable without
 * authentication. List endpoints return the lightweight
 * {@link ExperienceHighlightResponseDto} projection used for cards and
 * carousels, while the single-item lookups (by id or by slug) return the full
 * {@link ExperienceResponseDto} including experience detail.
 */
@RestController
@RequestMapping("/public/experiences")
@Tag(name = "Public Experience API", description = "Endpoints for browsing experiences")
public class ExperienceController {

    @Autowired
    private ExperienceService experienceService;

    /**
     * Lists all active experiences as highlight cards.
     *
     * @return {@code 200 OK} wrapping the list of
     *         {@link ExperienceHighlightResponseDto}
     */
    @GetMapping
    @Operation(summary = "Get All Active Experiences")
    public ResponseEntity<ApiResponse<?>> getAll() {
        List<ExperienceHighlightResponseDto> response = experienceService.getAllActive();
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Returns one experience with its full detail section.
     *
     * @param id identifier of the experience
     * @return {@code 200 OK} wrapping the {@link ExperienceResponseDto}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get Experience by ID (with full detail)")
    public ResponseEntity<ApiResponse<?>> getById(@PathVariable Long id) {
        ExperienceResponseDto response = experienceService.getById(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Returns one experience with its full detail section, addressed by the
     * URL-friendly slug used in storefront links.
     *
     * @param slug unique slug of the experience
     * @return {@code 200 OK} wrapping the {@link ExperienceResponseDto}
     */
    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get Experience by Slug (with full detail)")
    public ResponseEntity<ApiResponse<?>> getBySlug(@PathVariable String slug) {
        ExperienceResponseDto response = experienceService.getBySlug(slug);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Lists the experiences that belong to a sub-category, as highlight cards.
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
     * Lists the experiences flagged as featured, used for homepage banners.
     *
     * @return {@code 200 OK} wrapping the list of
     *         {@link ExperienceHighlightResponseDto}
     */
    @GetMapping("/featured")
    @Operation(summary = "Get Featured Experiences", description = "Returns featured active experiences for homepage banners")
    public ResponseEntity<ApiResponse<?>> getFeatured() {
        List<ExperienceHighlightResponseDto> response = experienceService.getFeatured();
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }
}
