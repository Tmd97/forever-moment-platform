package com.forvmom.core.controller.admin;

import com.forvmom.common.dto.request.*;
import com.forvmom.common.dto.response.*;
import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.common.utils.AppConstants;
import com.forvmom.core.services.LocationService;
import com.forvmom.core.services.ReorderingService;
import com.forvmom.data.entities.Location;
import com.forvmom.data.entities.TimeSlot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin endpoints for serviceable locations, their pincodes, and the junctions
 * that decide which catalog is offered where.
 *
 * <p>
 * Mapped under {@code /admin/locations} and intended for administrators only.
 * Besides location and pincode CRUD (deletes are soft), it manages three
 * many-to-many attachments: location to experience, location to category and
 * location to sub-category. Attachment rows carry their own display order,
 * active flag and, for experiences, an optional price override and validity
 * window.
 */
@RestController
@RequestMapping("/admin/locations")
@Tag(name = "Admin Location API", description = "Endpoints for managing locations and pincodes (Admin only)")
public class LocationControllerAdmin {

    @Autowired
    private ReorderingService reorderingService;

    @Autowired
    private LocationService locationService;

    /**
     * Creates a new serviceable location.
     *
     * @param requestDto the location to create
     * @return {@code 201 CREATED} wrapping the persisted
     *         {@link LocationResponseDto}
     */
    @PostMapping
    @Operation(summary = "Create Location", description = "Create a new serviceable location")
    public ResponseEntity<ApiResponse<?>> createLocation(@RequestBody LocationRequestDto requestDto) {
        LocationResponseDto response = locationService.createLocation(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildCreatedResponse(response, AppConstants.MSG_CREATED));
    }

    // TODO: can use flag from UI for including nested object or not(like pincodes)
    /**
     * Lists all locations, including inactive ones.
     *
     * @return {@code 200 OK} wrapping the list of {@link LocationResponseDto}
     */
    @GetMapping
    @Operation(summary = "Get All Locations", description = "Fetch all locations (including inactive)")
    public ResponseEntity<ApiResponse<?>> getAllLocations() {
        List<LocationResponseDto> response = locationService.getAll();
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Returns a single location together with its pincodes.
     *
     * @param id identifier of the location
     * @return {@code 200 OK} wrapping the {@link LocationResponseDto}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get Location by ID", description = "Fetch a location with its pincodes")
    public ResponseEntity<ApiResponse<?>> getLocationById(@PathVariable Long id) {
        LocationResponseDto response = locationService.getById(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Lists the locations registered for a city.
     *
     * @param city city name to filter by
     * @return {@code 200 OK} wrapping the list of {@link LocationResponseDto}
     */
    @GetMapping("/city/{city}")
    @Operation(summary = "Get Locations by City", description = "Fetch all locations in a city")
    public ResponseEntity<ApiResponse<?>> getLocationsByCity(@PathVariable String city) {
        List<LocationResponseDto> response = locationService.getByCity(city);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Updates an existing location.
     *
     * @param id         identifier of the location to update
     * @param requestDto the new location values
     * @return {@code 200 OK} wrapping the updated {@link LocationResponseDto}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update Location", description = "Update an existing location")
    public ResponseEntity<ApiResponse<?>> updateLocation(@PathVariable Long id,
            @RequestBody LocationRequestDto requestDto) {
        LocationResponseDto response = locationService.updateLocation(id, requestDto);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_UPDATED));
    }

    /**
     * Soft-deletes a location.
     *
     * @param id identifier of the location to delete
     * @return {@code 200 OK} with an empty payload
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Location", description = "Soft delete a location")
    public ResponseEntity<ApiResponse<?>> deleteLocation(@PathVariable Long id) {
        locationService.deleteLocation(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, AppConstants.MSG_DELETED));
    }

    /**
     * Flips the active flag of a location, controlling whether it is offered to
     * customers.
     *
     * @param id identifier of the location
     * @return {@code 200 OK} with an empty payload
     */
    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Toggle Location Active", description = "Toggle is_active for a location")
    public ResponseEntity<ApiResponse<?>> toggleLocation(@PathVariable Long id) {
        locationService.toggleActive(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, "Location status toggled successfully"));
    }

    /**
     * Adds a pincode to the location referenced in the request body.
     *
     * @param requestDto the pincode to create, including its owning location id
     * @return {@code 201 CREATED} wrapping the persisted {@link PincodeResponseDto}
     */
    @PostMapping("/pincodes")
    @Operation(summary = "Add Pincode", description = "Add a pincode to a location")
    public ResponseEntity<ApiResponse<?>> addPincode(@RequestBody PincodeRequestDto requestDto) {
        PincodeResponseDto response = locationService.addPincode(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildCreatedResponse(response, AppConstants.MSG_CREATED));
    }

    /**
     * Lists the pincodes belonging to a location.
     *
     * @param locationId identifier of the owning location
     * @return {@code 200 OK} wrapping the list of {@link PincodeResponseDto}
     */
    @GetMapping("/{locationId}/pincodes")
    @Operation(summary = "Get Pincodes by Location", description = "List all pincodes under a location")
    public ResponseEntity<ApiResponse<?>> getPincodesByLocation(@PathVariable Long locationId) {
        List<PincodeResponseDto> response = locationService.getPincodesByLocation(locationId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Updates a pincode's details.
     *
     * @param pincodeId  identifier of the pincode to update
     * @param requestDto the new pincode values
     * @return {@code 200 OK} wrapping the updated {@link PincodeResponseDto}
     */
    @PutMapping("/pincodes/{pincodeId}")
    @Operation(summary = "Update Pincode", description = "Update a pincode's details")
    public ResponseEntity<ApiResponse<?>> updatePincode(@PathVariable Long pincodeId,
            @RequestBody PincodeRequestDto requestDto) {
        PincodeResponseDto response = locationService.updatePincode(pincodeId, requestDto);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_UPDATED));
    }

    /**
     * Soft-deletes a pincode.
     *
     * @param pincodeId identifier of the pincode to delete
     * @return {@code 200 OK} with an empty payload
     */
    @DeleteMapping("/pincodes/{pincodeId}")
    @Operation(summary = "Delete Pincode", description = "Soft delete a pincode")
    public ResponseEntity<ApiResponse<?>> deletePincode(@PathVariable Long pincodeId) {
        locationService.deletePincode(pincodeId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, AppConstants.MSG_DELETED));
    }

    // ── Experience Association ────────────────────────────────────────────────

    /**
     * Lists the experiences this location is attached to.
     *
     * @param locationId identifier of the location
     * @return {@code 200 OK} wrapping the list of
     *         {@link ExperienceLocationResponseDto}
     */
    @GetMapping("/{locationId}/experiences")
    @Operation(summary = "Get Experiences for Location", description = "Lists all experiences this location is attached to")
    public ResponseEntity<ApiResponse<?>> getExperiencesForLocation(
            @PathVariable Long locationId) {
        List<ExperienceLocationResponseDto> response = locationService.getExperiencesForLocation(locationId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Attaches a location to an experience by creating the junction row. The body
     * is optional; when omitted an empty attachment is used, which leaves the price
     * override null so the experience's base price applies.
     *
     * @param locationId   identifier of the location
     * @param experienceId identifier of the experience
     * @param requestDto   optional price override and validity window
     * @return {@code 201 CREATED} wrapping the created
     *         {@link ExperienceLocationResponseDto}
     */
    @PostMapping("/{locationId}/experiences/{experienceId}")
    @Operation(summary = "Attach Location to Experience", description = "Creates an ExperienceLocationMapper row. "
            + "Optional body: priceOverride (null = use Experience.basePrice), validFrom, validTo")
    public ResponseEntity<ApiResponse<?>> attachToExperience(
            @PathVariable Long locationId,
            @PathVariable Long experienceId,
            @RequestBody(required = false) @Valid ExperienceLocationAttachRequestDto requestDto) {
        if (requestDto == null)
            requestDto = new ExperienceLocationAttachRequestDto();
        ExperienceLocationResponseDto response = locationService.attachToExperience(locationId, experienceId,
                requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildCreatedResponse(response, AppConstants.MSG_CREATED));
    }

    /**
     * Updates an existing location-to-experience attachment (price override, active
     * flag or validity dates).
     *
     * @param locationId   identifier of the location
     * @param experienceId identifier of the experience
     * @param requestDto   the new attachment values
     * @return {@code 200 OK} wrapping the updated
     *         {@link ExperienceLocationResponseDto}
     */
    @PutMapping("/{locationId}/experiences/{experienceId}")
    @Operation(summary = "Update Experience Attachment", description = "Updates priceOverride, isActive, or validity dates for an existing attachment")
    public ResponseEntity<ApiResponse<?>> updateExperienceAttachment(
            @PathVariable Long locationId,
            @PathVariable Long experienceId,
            @Valid @RequestBody ExperienceLocationAttachRequestDto requestDto) {
        ExperienceLocationResponseDto response = locationService.updateExperienceAttachment(locationId, experienceId,
                requestDto);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_UPDATED));
    }

    /**
     * Detaches a location from an experience by soft-deleting the junction row,
     * which also cascades to the timeslot mappings for that pair.
     *
     * @param locationId   identifier of the location
     * @param experienceId identifier of the experience
     * @return {@code 200 OK} with an empty payload
     */
    @DeleteMapping("/{locationId}/experiences/{experienceId}")
    @Operation(summary = "Detach Location from Experience", description = "Soft-deletes the junction row (and cascades to timeslot mappings for this pair)")
    public ResponseEntity<ApiResponse<?>> detachFromExperience(
            @PathVariable Long locationId,
            @PathVariable Long experienceId) {
        locationService.detachFromExperience(locationId, experienceId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, AppConstants.MSG_DELETED));
    }

    /**
     * Flips the active flag on a location-to-experience attachment.
     *
     * @param locationId identifier of the location; part of the path only, the
     *                   junction row is resolved from {@code mapperId}
     * @param mapperId   identifier of the attachment row to toggle
     * @return {@code 200 OK} with an empty payload
     */
    @PatchMapping("/{locationId}/experiences/{mapperId}/toggle")
    @Operation(summary = "Toggle Experience Attachment Active", description = "Toggles is_active on the ExperienceLocationMapper row by its mapperId")
    public ResponseEntity<ApiResponse<?>> toggleExperienceAttachmentActive(
            @PathVariable Long locationId,
            @PathVariable Long mapperId) {
        locationService.toggleExperienceAttachmentActive(mapperId);
        return ResponseEntity.ok(
                ResponseUtil.buildOkResponse(null, "Location-Experience attachment status toggled"));
    }

    /**
     * Moves a location to a new 1-based display position, shifting the locations it
     * passes over so positions stay contiguous.
     *
     * @param reorderRequestDto carries the location id and its target position
     * @return {@code 200 OK} with an empty payload
     */
    @PatchMapping("/reorder")
    public ResponseEntity<ApiResponse<?>> reOrderTheItems(
            @RequestBody ReorderRequestDto reorderRequestDto) {
        reorderingService.reorderItems(reorderRequestDto.getId(), reorderRequestDto.getNewPosition(), Location.class);
        return ResponseEntity.ok(
                ResponseUtil.buildOkResponse(null, AppConstants.MSG_UPDATED));
    }

    // ── Category Association ───────────────────────────────────────────────

    /**
     * Lists the categories this location is attached to.
     *
     * @param locationId identifier of the location
     * @return {@code 200 OK} wrapping the list of
     *         {@link CategoryLocationResponseDto}
     */
    @GetMapping("/{locationId}/categories")
    @Operation(summary = "Get Categories for Location", description = "Lists all categories this location is attached to")
    public ResponseEntity<ApiResponse<?>> getCategoriesForLocation(
            @PathVariable Long locationId) {
        List<CategoryLocationResponseDto> response = locationService.getCategoriesForLocation(locationId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Attaches a category to a location by creating the junction row. The body is
     * optional; when omitted an empty attachment (default display order and active
     * flag) is used.
     *
     * @param locationId identifier of the location
     * @param categoryId identifier of the category
     * @param requestDto optional display order and active flag
     * @return {@code 201 CREATED} wrapping the created
     *         {@link CategoryLocationResponseDto}
     */
    @PostMapping("/{locationId}/categories/{categoryId}")
    @Operation(summary = "Attach Category to Location", description = "Creates a CategoryLocationMapper row. Optional body: displayOrder, isActive")
    public ResponseEntity<ApiResponse<?>> attachCategoryToLocation(
            @PathVariable Long locationId,
            @PathVariable Long categoryId,
            @RequestBody(required = false) @Valid CategoryLocationAttachRequestDto requestDto) {
        if (requestDto == null) requestDto = new CategoryLocationAttachRequestDto();
        CategoryLocationResponseDto response = locationService.attachCategoryToLocation(locationId, categoryId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildCreatedResponse(response, AppConstants.MSG_CREATED));
    }

    /**
     * Updates an existing location-to-category attachment (display order or active
     * flag).
     *
     * @param locationId identifier of the location
     * @param categoryId identifier of the category
     * @param requestDto the new attachment values
     * @return {@code 200 OK} wrapping the updated
     *         {@link CategoryLocationResponseDto}
     */
    @PutMapping("/{locationId}/categories/{categoryId}")
    @Operation(summary = "Update Category Attachment", description = "Updates displayOrder or isActive for an existing attachment")
    public ResponseEntity<ApiResponse<?>> updateCategoryAttachment(
            @PathVariable Long locationId,
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryLocationAttachRequestDto requestDto) {
        CategoryLocationResponseDto response = locationService.updateCategoryAttachment(locationId, categoryId, requestDto);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_UPDATED));
    }

    /**
     * Detaches a category from a location by soft-deleting the junction row.
     *
     * @param locationId identifier of the location
     * @param categoryId identifier of the category
     * @return {@code 200 OK} with an empty payload
     */
    @DeleteMapping("/{locationId}/categories/{categoryId}")
    @Operation(summary = "Detach Category from Location", description = "Soft-deletes the junction row")
    public ResponseEntity<ApiResponse<?>> detachCategoryFromLocation(
            @PathVariable Long locationId,
            @PathVariable Long categoryId) {
        locationService.detachCategoryFromLocation(locationId, categoryId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, AppConstants.MSG_DELETED));
    }

    /**
     * Flips the active flag on a location-to-category attachment.
     *
     * @param locationId identifier of the location; part of the path only, the
     *                   junction row is resolved from {@code mapperId}
     * @param mapperId   identifier of the attachment row to toggle
     * @return {@code 200 OK} with an empty payload
     */
    @PatchMapping("/{locationId}/categories/{mapperId}/toggle")
    @Operation(summary = "Toggle Category Attachment Active", description = "Toggles is_active on the CategoryLocationMapper row by its mapperId")
    public ResponseEntity<ApiResponse<?>> toggleCategoryAttachmentActive(
            @PathVariable Long locationId,
            @PathVariable Long mapperId) {
        locationService.toggleCategoryAttachmentActive(mapperId);
        return ResponseEntity.ok(
                ResponseUtil.buildOkResponse(null, "Location-Category attachment status toggled"));
    }

    // ── SubCategory Association ───────────────────────────────────────────

    /**
     * Lists the sub-categories this location is attached to.
     *
     * @param locationId identifier of the location
     * @return {@code 200 OK} wrapping the list of
     *         {@link SubCategoryLocationResponseDto}
     */
    @GetMapping("/{locationId}/subcategories")
    @Operation(summary = "Get SubCategories for Location", description = "Lists all subcategories this location is attached to")
    public ResponseEntity<ApiResponse<?>> getSubCategoriesForLocation(
            @PathVariable Long locationId) {
        List<SubCategoryLocationResponseDto> response = locationService.getSubCategoriesForLocation(locationId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Attaches a sub-category to a location by creating the junction row. The body
     * is optional; when omitted an empty attachment (default display order and
     * active flag) is used.
     *
     * @param locationId    identifier of the location
     * @param subCategoryId identifier of the sub-category
     * @param requestDto    optional display order and active flag
     * @return {@code 201 CREATED} wrapping the created
     *         {@link SubCategoryLocationResponseDto}
     */
    @PostMapping("/{locationId}/subcategories/{subCategoryId}")
    @Operation(summary = "Attach SubCategory to Location", description = "Creates a SubCategoryLocationMapper row. Optional body: displayOrder, isActive")
    public ResponseEntity<ApiResponse<?>> attachSubCategoryToLocation(
            @PathVariable Long locationId,
            @PathVariable Long subCategoryId,
            @RequestBody(required = false) @Valid SubCategoryLocationAttachRequestDto requestDto) {
        if (requestDto == null) requestDto = new SubCategoryLocationAttachRequestDto();
        SubCategoryLocationResponseDto response = locationService.attachSubCategoryToLocation(locationId, subCategoryId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildCreatedResponse(response, AppConstants.MSG_CREATED));
    }

    /**
     * Updates an existing location-to-sub-category attachment (display order or
     * active flag).
     *
     * @param locationId    identifier of the location
     * @param subCategoryId identifier of the sub-category
     * @param requestDto    the new attachment values
     * @return {@code 200 OK} wrapping the updated
     *         {@link SubCategoryLocationResponseDto}
     */
    @PutMapping("/{locationId}/subcategories/{subCategoryId}")
    @Operation(summary = "Update SubCategory Attachment", description = "Updates displayOrder or isActive for an existing attachment")
    public ResponseEntity<ApiResponse<?>> updateSubCategoryAttachment(
            @PathVariable Long locationId,
            @PathVariable Long subCategoryId,
            @Valid @RequestBody SubCategoryLocationAttachRequestDto requestDto) {
        SubCategoryLocationResponseDto response = locationService.updateSubCategoryAttachment(locationId, subCategoryId, requestDto);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_UPDATED));
    }

    /**
     * Detaches a sub-category from a location by soft-deleting the junction row.
     *
     * @param locationId    identifier of the location
     * @param subCategoryId identifier of the sub-category
     * @return {@code 200 OK} with an empty payload
     */
    @DeleteMapping("/{locationId}/subcategories/{subCategoryId}")
    @Operation(summary = "Detach SubCategory from Location", description = "Soft-deletes the junction row")
    public ResponseEntity<ApiResponse<?>> detachSubCategoryFromLocation(
            @PathVariable Long locationId,
            @PathVariable Long subCategoryId) {
        locationService.detachSubCategoryFromLocation(locationId, subCategoryId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, AppConstants.MSG_DELETED));
    }

    /**
     * Flips the active flag on a location-to-sub-category attachment.
     *
     * @param locationId identifier of the location; part of the path only, the
     *                   junction row is resolved from {@code mapperId}
     * @param mapperId   identifier of the attachment row to toggle
     * @return {@code 200 OK} with an empty payload
     */
    @PatchMapping("/{locationId}/subcategories/{mapperId}/toggle")
    @Operation(summary = "Toggle SubCategory Attachment Active", description = "Toggles is_active on the SubCategoryLocationMapper row by its mapperId")
    public ResponseEntity<ApiResponse<?>> toggleSubCategoryAttachmentActive(
            @PathVariable Long locationId,
            @PathVariable Long mapperId) {
        locationService.toggleSubCategoryAttachmentActive(mapperId);
        return ResponseEntity.ok(
                ResponseUtil.buildOkResponse(null, "Location-SubCategory attachment status toggled"));
    }
}
