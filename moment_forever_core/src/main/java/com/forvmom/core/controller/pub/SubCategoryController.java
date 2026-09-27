package com.forvmom.core.controller.pub;

import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.common.utils.AppConstants;
import com.forvmom.common.dto.response.SubCategoryResponseDto;
import com.forvmom.core.services.SubCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * Read-only catalog browsing endpoints for sub-categories.
 *
 * <p>
 * Mapped under {@code /public/subcategories} and reachable without
 * authentication. In addition to id lookup it supports lookup by the
 * URL-friendly {@code slug}, which is what the storefront uses for shareable
 * links, and listing by parent category id.
 */
@RestController
@RequestMapping("/public/subcategories")
@Tag(name = "Public SubCategory API", description = "Endpoints for browsing sub-categories")
public class SubCategoryController {

    @Autowired
    private SubCategoryService subCategoryService;

    /**
     * Returns a single sub-category by its identifier.
     *
     * @param id identifier of the sub-category to fetch
     * @return {@code 200 OK} wrapping the {@link SubCategoryResponseDto}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get SubCategory by ID", description = "Fetch a single sub-category by its unique ID")
    public ResponseEntity<ApiResponse<?>> getSubCategoryById(@PathVariable Long id) {
        SubCategoryResponseDto subCategoryResponse = subCategoryService.getById(id);
        return ResponseEntity.ok(
                ResponseUtil.buildOkResponse(subCategoryResponse, AppConstants.MSG_FETCHED));
    }

    /**
     * Returns a single sub-category by its URL-friendly slug.
     *
     * @param slug unique slug of the sub-category
     * @return {@code 200 OK} wrapping the {@link SubCategoryResponseDto}
     */
    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get SubCategory by Slug", description = "Fetch a single sub-category by its URL-friendly slug")
    public ResponseEntity<ApiResponse<?>> getSubCategoryBySlug(@PathVariable String slug) {
        SubCategoryResponseDto subCategoryResponse = subCategoryService.getBySlug(slug);
        return ResponseEntity.ok(
                ResponseUtil.buildOkResponse(subCategoryResponse, AppConstants.MSG_FETCHED));
    }

    /**
     * Returns every sub-category in the catalog.
     *
     * @return {@code 200 OK} wrapping the list of {@link SubCategoryResponseDto}
     */
    @GetMapping
    @Operation(summary = "Get All SubCategories", description = "Fetch a list of all available sub-categories")
    public ResponseEntity<ApiResponse<?>> getAllSubCategories() {
        List<SubCategoryResponseDto> subCategoryDtos = subCategoryService.getAll();
        return ResponseEntity.ok(
                ResponseUtil.buildOkResponse(subCategoryDtos, AppConstants.MSG_FETCHED));
    }

    /**
     * Returns the sub-categories that belong to a given parent category.
     *
     * @param categoryId identifier of the parent category
     * @return {@code 200 OK} wrapping the list of {@link SubCategoryResponseDto}
     */
    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get SubCategories by Category", description = "Fetch all sub-categories belonging to a specific parent category")
    public ResponseEntity<ApiResponse<?>> getSubCategoriesByCategory(@PathVariable Long categoryId) {
        List<SubCategoryResponseDto> subCategoryDtoList = subCategoryService.getByCategoryId(categoryId);
        return ResponseEntity.ok(
                ResponseUtil.buildOkResponse(subCategoryDtoList, AppConstants.MSG_FETCHED));
    }
}