package com.forvmom.core.controller.pub;

import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.common.utils.AppConstants;
import com.forvmom.common.dto.response.CategoryResponseDto;
import com.forvmom.core.services.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * Read-only catalog browsing endpoints for categories.
 *
 * <p>
 * Mapped under {@code /public/categories}, so it is reachable without
 * authentication through the API Gateway. Responses are the same
 * {@link CategoryResponseDto} projections used by the admin API, but this
 * controller exposes fetch operations only - all mutation lives in the admin
 * category controller.
 */
@RestController
@RequestMapping("/public/categories")
@Tag(name = "Public Category API", description = "Endpoints for browsing categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * Returns a single category, including its nested sub-categories when present.
     *
     * @param id identifier of the category to fetch
     * @return {@code 200 OK} wrapping the {@link CategoryResponseDto}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get Category by ID", description = "Fetch a single category by its unique ID")
    public ResponseEntity<ApiResponse<?>> getCategoryById(@PathVariable Long id) {
        CategoryResponseDto categoryResponse = categoryService.getById(id);
        return ResponseEntity.ok(
                ResponseUtil.buildOkResponse(categoryResponse, AppConstants.MSG_FETCHED));
    }

    /**
     * Returns every category known to the catalog, each with its sub-categories.
     * An empty list is returned when no categories exist.
     *
     * @return {@code 200 OK} wrapping the list of {@link CategoryResponseDto}
     */
    @GetMapping
    @Operation(summary = "Get All Categories", description = "Fetch a list of all available categories")
    public ResponseEntity<ApiResponse<?>> getAllCategories() {
        List<CategoryResponseDto> categoryDtos = categoryService.getAll();
        return ResponseEntity.ok(
                ResponseUtil.buildOkResponse(categoryDtos, AppConstants.MSG_FETCHED));
    }
}