package com.forvmom.core.controller.admin;

import com.forvmom.common.dto.request.BulkAttachMediaRequestDto;
import com.forvmom.common.dto.request.ExperienceMediaAttachRequestDto;
import com.forvmom.common.dto.response.BulkAttachMediaResultDto;
import com.forvmom.common.dto.response.ExperienceMediaResponseDto;
import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.common.utils.AppConstants;
import com.forvmom.core.services.ExperienceMediaService;
import com.forvmom.core.services.ImageService;
import com.forvmom.core.services.MediaService;
import com.forvmom.store.dto.ImageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Admin controller for attaching/detaching/updating Media on an Experience.
 *
 * URL scheme (mirrors ExperienceTimeSlotControllerAdmin):
 * /admin/experiences/{experienceId}/media
 */
@RestController
@RequestMapping("/admin/experiences/{experienceId}/media")
@Tag(name = "Admin Experience Media API", description = "Attach, update and detach images/videos on an experience (Admin only)")
public class ExperienceMediaControllerAdmin {

    private static final Logger logger = LoggerFactory.getLogger(ExperienceMediaControllerAdmin.class);

    @Autowired
    private ExperienceMediaService experienceMediaService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private MediaService mediaService;

    // ── Read ──────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Get Media for Experience", description = "Returns all media attached to an experience, ordered by displayOrder")
    public ResponseEntity<ApiResponse<List<ExperienceMediaResponseDto>>> getMediaForExperience(
            @PathVariable Long experienceId) {
        List<ExperienceMediaResponseDto> response = experienceMediaService.getMediaForExperience(experienceId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    @GetMapping("/primary")
    @Operation(summary = "Get Primary (Hero) Image for Experience")
    public ResponseEntity<ApiResponse<ExperienceMediaResponseDto>> getPrimaryMedia(
            @PathVariable Long experienceId) {
        ExperienceMediaResponseDto response = experienceMediaService.getPrimaryMedia(experienceId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    // ── Attach ────────────────────────────────────────────────────────────────

    @PostMapping("/{mediaId}")
    @Operation(summary = "Attach Media to Experience", description = "Links a Media record to an experience. "
            + "Set isPrimary=true to make it the cover image (demotes existing primary).")
    public ResponseEntity<ApiResponse<ExperienceMediaResponseDto>> attachMedia(
            @PathVariable Long experienceId,
            @PathVariable Long mediaId,
            @RequestBody(required = false) @Valid ExperienceMediaAttachRequestDto requestDto) {
        if (requestDto == null)
            requestDto = new ExperienceMediaAttachRequestDto();
        ExperienceMediaResponseDto response = experienceMediaService.attachMedia(
                experienceId, mediaId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildCreatedResponse(response, AppConstants.MSG_CREATED));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and Attach Media to Experience", description = "Uploads an image and immediately attaches it to the experience in one API call. "
            + "If attach fails after upload, uploaded media is cleaned up.")
    public ResponseEntity<ApiResponse<ExperienceMediaResponseDto>> uploadAndAttachMedia(
            @PathVariable Long experienceId,
            @RequestParam("file") MultipartFile file,
            @RequestParam Map<String, Object> metadata,
            @RequestPart(value = "attach", required = false) @Valid ExperienceMediaAttachRequestDto attachRequest) {

        if (attachRequest == null) {
            attachRequest = new ExperienceMediaAttachRequestDto();
        }

        ImageResponse uploaded = imageService.uploadImage(file, metadata);
        try {
            ExperienceMediaResponseDto response = experienceMediaService.attachMedia(
                    experienceId,
                    uploaded.getId(),
                    attachRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseUtil.buildCreatedResponse(response, AppConstants.MSG_CREATED));
        } catch (RuntimeException exception) {
            cleanupUploadedMedia(uploaded.getId());
            throw exception;
        }
    }

    @PostMapping("/bulk-attach")
    @Operation(summary = "Bulk Attach Media to Experience", description = "Attaches multiple images/videos to an experience in one request. "
            + "Each item holds a mediaId plus optional junction overrides (displayOrder, isPrimary, etc). "
            + "Duplicates and not-found IDs are reported in the 'skipped' list instead of failing the whole operation.")
    public ResponseEntity<ApiResponse<BulkAttachMediaResultDto>> bulkAttachMedia(
            @PathVariable Long experienceId,
            @Valid @RequestBody BulkAttachMediaRequestDto requestDto) {
        BulkAttachMediaResultDto result = experienceMediaService.bulkAttachMedia(experienceId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildCreatedResponse(result, "Bulk attach completed"));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @PutMapping("/{mediaId}")
    @Operation(summary = "Update Media Attachment", description = "Updates displayOrder, isPrimary, altText or isActive on the junction row")
    public ResponseEntity<ApiResponse<ExperienceMediaResponseDto>> updateAttachment(
            @PathVariable Long experienceId,
            @PathVariable Long mediaId,
            @Valid @RequestBody ExperienceMediaAttachRequestDto requestDto) {
        ExperienceMediaResponseDto response = experienceMediaService.updateAttachment(
                experienceId, mediaId, requestDto);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_UPDATED));
    }

    // ── Detach ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Detach Media from Experience", description = "Soft-deletes the junction row. The Media master record is NOT deleted.")
    public ResponseEntity<ApiResponse<Void>> detachMedia(
            @PathVariable Long experienceId,
            @PathVariable Long mediaId) {
        experienceMediaService.detachMedia(experienceId, mediaId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, AppConstants.MSG_DELETED));
    }

    @PatchMapping("/{mapperId}/toggle")
    @Operation(summary = "Toggle Media Attachment Active", description = "Toggles isActive on the junction row by its mapperId")
    public ResponseEntity<ApiResponse<Void>> toggleAttachmentActive(
            @PathVariable Long experienceId,
            @PathVariable Long mapperId) {
        experienceMediaService.toggleAttachmentActive(mapperId);
        return ResponseEntity.ok(
                ResponseUtil.buildOkResponse(null, "Media mapping status toggled successfully"));
    }

    private void cleanupUploadedMedia(Long mediaId) {
        try {
            mediaService.deleteMediaWithStorage(mediaId);
        } catch (RuntimeException cleanupException) {
            logger.error("Failed cleanup for uploaded mediaId={} after attach failure", mediaId, cleanupException);
        }
    }
}
