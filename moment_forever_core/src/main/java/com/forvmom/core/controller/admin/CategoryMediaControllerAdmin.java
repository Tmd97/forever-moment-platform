package com.forvmom.core.controller.admin;

import com.forvmom.common.dto.request.ExperienceMediaAttachRequestDto;
import com.forvmom.common.dto.response.ExperienceMediaResponseDto;
import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.common.utils.AppConstants;
import com.forvmom.core.services.CatalogMediaService;
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

@RestController
@RequestMapping("/admin/categories/{categoryId}/media")
@Tag(name = "Admin Category Media API", description = "Attach, update and detach media on a category (Admin only)")
public class CategoryMediaControllerAdmin {

    private static final Logger logger = LoggerFactory.getLogger(CategoryMediaControllerAdmin.class);

    @Autowired
    private CatalogMediaService catalogMediaService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private MediaService mediaService;

    @GetMapping
    @Operation(summary = "Get Media for Category")
    public ResponseEntity<ApiResponse<List<ExperienceMediaResponseDto>>> getMediaForCategory(@PathVariable Long categoryId) {
        List<ExperienceMediaResponseDto> response = catalogMediaService.getCategoryMedia(categoryId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    @GetMapping("/primary")
    @Operation(summary = "Get Primary Image for Category")
    public ResponseEntity<ApiResponse<ExperienceMediaResponseDto>> getPrimaryMedia(@PathVariable Long categoryId) {
        ExperienceMediaResponseDto response = catalogMediaService.getCategoryPrimaryMedia(categoryId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    @PostMapping("/{mediaId}")
    @Operation(summary = "Attach Media to Category")
    public ResponseEntity<ApiResponse<ExperienceMediaResponseDto>> attachMedia(
            @PathVariable Long categoryId,
            @PathVariable Long mediaId,
            @RequestBody(required = false) @Valid ExperienceMediaAttachRequestDto requestDto) {
        if (requestDto == null) {
            requestDto = new ExperienceMediaAttachRequestDto();
        }
        ExperienceMediaResponseDto response = catalogMediaService.attachMediaToCategory(categoryId, mediaId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildCreatedResponse(response, AppConstants.MSG_CREATED));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and Attach Media to Category")
    public ResponseEntity<ApiResponse<ExperienceMediaResponseDto>> uploadAndAttachMedia(
            @PathVariable Long categoryId,
            @RequestParam("file") MultipartFile file,
            @RequestParam Map<String, Object> metadata,
            @RequestPart(value = "attach", required = false) @Valid ExperienceMediaAttachRequestDto attachRequest) {

        if (attachRequest == null) {
            attachRequest = new ExperienceMediaAttachRequestDto();
        }
        ImageResponse uploaded = imageService.uploadImage(file, metadata);
        try {
            ExperienceMediaResponseDto response = catalogMediaService.attachMediaToCategory(
                    categoryId,
                    uploaded.getId(),
                    attachRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseUtil.buildCreatedResponse(response, AppConstants.MSG_CREATED));
        } catch (RuntimeException exception) {
            cleanupUploadedMedia(uploaded.getId());
            throw exception;
        }
    }

    @PutMapping("/{mediaId}")
    @Operation(summary = "Update Category Media Attachment")
    public ResponseEntity<ApiResponse<ExperienceMediaResponseDto>> updateAttachment(
            @PathVariable Long categoryId,
            @PathVariable Long mediaId,
            @Valid @RequestBody ExperienceMediaAttachRequestDto requestDto) {
        ExperienceMediaResponseDto response = catalogMediaService.updateCategoryAttachment(categoryId, mediaId, requestDto);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_UPDATED));
    }

    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Detach Media from Category")
    public ResponseEntity<ApiResponse<Void>> detachMedia(
            @PathVariable Long categoryId,
            @PathVariable Long mediaId) {
        catalogMediaService.detachMediaFromCategory(categoryId, mediaId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, AppConstants.MSG_DELETED));
    }

    @PatchMapping("/{mapperId}/toggle")
    @Operation(summary = "Toggle Category Media Attachment Active")
    public ResponseEntity<ApiResponse<Void>> toggleAttachmentActive(
            @PathVariable Long categoryId,
            @PathVariable Long mapperId) {
        catalogMediaService.toggleCategoryAttachmentActive(categoryId, mapperId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, "Category media mapping status toggled successfully"));
    }

    private void cleanupUploadedMedia(Long mediaId) {
        try {
            mediaService.deleteMediaWithStorage(mediaId);
        } catch (RuntimeException cleanupException) {
            logger.error("Failed cleanup for uploaded mediaId={} after category attach failure", mediaId, cleanupException);
        }
    }
}
