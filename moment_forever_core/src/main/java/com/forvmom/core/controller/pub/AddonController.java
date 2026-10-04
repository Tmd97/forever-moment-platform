package com.forvmom.core.controller.pub;

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
@RequestMapping("/public")
@Tag(name = "Admin Addon API", description = "Master CRUD for addons and per-experience attachment with price override (Admin only)")
public class AddonController {

    private static final Logger logger = LoggerFactory.getLogger(AddonController.class);

    private final AddonService addonService;
    private final ImageService imageService;
    private final MediaService mediaService;

    public AddonController(
            AddonService addonService,
            ImageService imageService,
            MediaService mediaService) {
        this.addonService = addonService;
        this.imageService = imageService;
        this.mediaService = mediaService;
    }

    // ── Master Addon CRUD ─────────────────────────────────────────────────────

    @GetMapping("/addons")
    @Operation(summary = "Get All Addons", description = "Returns all master addon records")
    public ResponseEntity<ApiResponse<?>> getAllAddons() {
        List<AddonResponseDto> result = addonService.getAllAddons();
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(result, "Addons fetched successfully"));
    }

    // ── Experience Attachment ─────────────────────────────────────────────────

    @GetMapping("/experiences/{experienceId}/addons")
    @Operation(summary = "Get Addons for Experience", description = "Lists all addons attached to an experience. Each item includes mapperId — use this as addonMapperIds in the booking request.")
    public ResponseEntity<ApiResponse<?>> getAddonsForExperience(@PathVariable Long experienceId) {
        List<ExperienceAddonResponseDto> result = addonService.getAddonsForExperience(experienceId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(result, "Addons fetched successfully"));
    }

}
