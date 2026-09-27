package com.forvmom.core.controller.admin;

import com.forvmom.common.dto.request.AddonRequestDto;
import com.forvmom.common.dto.request.BulkAttachAddonRequestDto;
import com.forvmom.common.dto.response.AddonResponseDto;
import com.forvmom.common.dto.response.BulkAttachAddonResultDto;
import com.forvmom.common.dto.response.ExperienceAddonResponseDto;
import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.core.services.AddonService;
import com.forvmom.core.services.ImageService;
import com.forvmom.core.services.MediaService;
import com.forvmom.store.dto.ImageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Admin controller for managing master Addon records
 * and attaching/detaching them to specific experiences.
 *
 * <p>
 * Master CRUD → /api/admin/addons
 * <p>
 * Attachment → /api/admin/experiences/{experienceId}/addons
 *
 */
@RestController
@RequestMapping("/admin")
@Tag(name = "Admin Addon API", description = "Master CRUD for addons and per-experience attachment with price override (Admin only)")
public class AddonControllerAdmin {

    private static final Logger logger = LoggerFactory.getLogger(AddonControllerAdmin.class);

    private final AddonService addonService;
    private final ImageService imageService;
    private final MediaService mediaService;

    public AddonControllerAdmin(
            AddonService addonService,
            ImageService imageService,
            MediaService mediaService) {
        this.addonService = addonService;
        this.imageService = imageService;
        this.mediaService = mediaService;
    }

    // ── Master Addon CRUD ─────────────────────────────────────────────────────

    @PostMapping("/addons")
    @Operation(summary = "Create Addon", description = "Creates a reusable master addon record")
    public ResponseEntity<ApiResponse<?>> createAddon(@Valid @RequestBody AddonRequestDto requestDto) {
        AddonResponseDto result = addonService.createAddon(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildCreatedResponse(result, "Addon created successfully"));
    }

    @GetMapping("/addons")
    @Operation(summary = "Get All Addons", description = "Returns all master addon records")
    public ResponseEntity<ApiResponse<?>> getAllAddons() {
        List<AddonResponseDto> result = addonService.getAllAddons();
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(result, "Addons fetched successfully"));
    }

    @PutMapping("/addons/{id}")
    @Operation(summary = "Update Addon")
    public ResponseEntity<ApiResponse<?>> updateAddon(
            @PathVariable Long id,
            @Valid @RequestBody AddonRequestDto requestDto) {
        AddonResponseDto result = addonService.updateAddon(id, requestDto);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(result, "Addon updated successfully"));
    }

    @PostMapping(value = "/addons/{id}/image/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and Attach Image to Addon", description = "Uploads an image and immediately attaches it to the addon in one API call. "
            + "If addon attach fails after upload, uploaded media is cleaned up.")
    public ResponseEntity<ApiResponse<?>> uploadAndAttachAddonImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam Map<String, Object> metadata) {
        ImageResponse uploaded = imageService.uploadImage(file, metadata);
        try {
            AddonResponseDto result = addonService.updateAddonImage(id, uploaded.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseUtil.buildCreatedResponse(result, "Addon image uploaded and attached successfully"));
        } catch (RuntimeException exception) {
            cleanupUploadedMedia(uploaded.getId());
            throw exception;
        }
    }

    @DeleteMapping("/addons/{id}")
    @Operation(summary = "Delete Addon (soft)", description = "Soft-deletes the master addon and all its junction rows")
    public ResponseEntity<Void> deleteAddon(@PathVariable Long id) {
        addonService.deleteAddon(id);
        return ResponseEntity.noContent().build();
    }

    // ── Experience Attachment ─────────────────────────────────────────────────

    @GetMapping("/experiences/{experienceId}/addons")
    @Operation(summary = "Get Addons for Experience", description = "Lists all addons attached to an experience. Each item includes mapperId — use this as addonMapperIds in the booking request.")
    public ResponseEntity<ApiResponse<?>> getAddonsForExperience(@PathVariable Long experienceId) {
        List<ExperienceAddonResponseDto> result = addonService.getAddonsForExperience(experienceId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(result, "Addons fetched successfully"));
    }

    @PostMapping("/experiences/{experienceId}/addons/{addonId}")
    @Operation(summary = "Attach Addon to Experience", description = "Links a master addon to an experience. Optional priceOverride and isFree params. "
            + "Returns the created mapping with mapperId.")
    public ResponseEntity<ApiResponse<?>> attachAddonToExperience(
            @PathVariable Long experienceId,
            @PathVariable Long addonId,
            @RequestParam(required = false) BigDecimal priceOverride,
            @RequestParam(required = false, defaultValue = "false") Boolean isFree) {
        ExperienceAddonResponseDto result = addonService.attachToExperience(experienceId, addonId,
                priceOverride, isFree);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildCreatedResponse(result, "Addon attached successfully"));
    }

    @PostMapping("/experiences/{experienceId}/addons/bulk-attach")
    @Operation(summary = "Bulk Attach Addons to Experience", description = "Attaches multiple addons to an experience in one call. "
            + "Skips (does not fail for) already-attached or not-found addons. "
            + "Returns attached and skipped lists.")
    public ResponseEntity<ApiResponse<?>> bulkAttachAddons(
            @PathVariable Long experienceId,
            @Valid @RequestBody BulkAttachAddonRequestDto requestDto) {
        BulkAttachAddonResultDto result = addonService.attachAddons(experienceId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildCreatedResponse(result, "Bulk attach completed"));
    }

    @DeleteMapping("/experiences/{experienceId}/addons/{addonId}")
    @Operation(summary = "Detach Addon from Experience", description = "Removes the junction row only; master addon record is NOT deleted")
    public ResponseEntity<Void> detachAddonFromExperience(
            @PathVariable Long experienceId,
            @PathVariable Long addonId) {
        addonService.detachFromExperience(experienceId, addonId);
        return ResponseEntity.noContent().build();
    }

    private void cleanupUploadedMedia(Long mediaId) {
        try {
            mediaService.deleteMediaWithStorage(mediaId);
        } catch (RuntimeException cleanupException) {
            logger.error("Failed cleanup for uploaded mediaId={} after addon attach failure", mediaId, cleanupException);
        }
    }
}
