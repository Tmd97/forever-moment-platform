package com.forvmom.core.controller.admin;

import com.forvmom.common.dto.request.MediaRequestDto;
import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.common.utils.AppConstants;
import com.forvmom.core.services.ImageService;
import com.forvmom.core.services.MediaService;
import com.forvmom.store.dto.ImageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Admin controller for image upload and management.
 * <p>
 * All read / update / delete operations address images by their SQL Media.id
 * (Long).
 * The download endpoint resolves the GridFS filePath from SQL and streams the
 * file.
 * <p>
 * Public download URL (cache-busted):
 * GET /images/{storageFileName} — exposed by the public controller (not here)
 */
@RestController
@RequestMapping("/admin/images")
@Tag(name = "Admin Image API", description = "Upload and manage images (Admin only)")
public class ImageControllerAdmin {

    @Autowired
    private ImageService imageService;

    @Autowired
    private MediaService mediaService;

    // ── Upload (single) ───────────────────────────────────────────────────────

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload Image", description = "Uploads to GridFS and creates a Media SQL record. "
            + "storageFileName = originalName_<timestamp> for cache-busting.")
    public ResponseEntity<ApiResponse<ImageResponse>> uploadImage(
            @RequestParam("file") @Parameter(description = "Image file", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)) MultipartFile file,
            @RequestParam Map<String, Object> metadata) {

        ImageResponse response = imageService.uploadImage(file, metadata);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildCreatedResponse(response, "Image uploaded successfully"));
    }

    // ── Upload (batch) ────────────────────────────────────────────────────────

    @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Batch Upload Images", description = "Uploads multiple files in one request. Each file is stored in GridFS "
            + "and gets its own Media SQL record. Returns the list of created records.")
    public ResponseEntity<ApiResponse<List<ImageResponse>>> batchUploadImages(
            @RequestPart("files") List<MultipartFile> files,

            @RequestParam Map<String, Object> metadata) {

        List<ImageResponse> results = new ArrayList<>();
        for (MultipartFile file : files) {
            results.add(imageService.uploadImage(file, metadata));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildCreatedResponse(results,
                        files.size() + " image(s) uploaded successfully"));
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List All Images", description = "Returns all active media records with public URLs")
    public ResponseEntity<ApiResponse<List<ImageResponse>>> getAllImages() {
        List<ImageResponse> response = mediaService.getAllMedia();
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Image by SQL ID")
    public ResponseEntity<ApiResponse<ImageResponse>> getImageById(@PathVariable Long id) {
        ImageResponse response = mediaService.getMediaById(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    // ── Download (single) ─────────────────────────────────────────────────────

    @GetMapping("/{id}/download")
    @Operation(summary = "Download Image by SQL ID", description = "Looks up the GridFS filePath from the Media SQL record and streams the file")
    public ResponseEntity<Resource> downloadImage(@PathVariable Long id) throws IOException {
        ImageResponse media = mediaService.getMediaById(id);
        String gridFsId = media.getFilePath();

        Resource resource = imageService.downloadImage(gridFsId);
        String contentType = imageService.getContentType(gridFsId)
                .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(resource.contentLength())
                .body(resource);
    }

    @GetMapping("/fetch/{storageFileName}")
    @Operation(summary = "Fetch Image by storageFileName", description = "Looks up the GridFS filePath from the Media SQL record and streams the file based on cache busted storageFileName")
    public ResponseEntity<Resource> downloadImageWithCacheBust(
            @PathVariable String storageFileName,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) throws IOException {
        String eTag = "\"" + storageFileName + "\"";
        CacheControl cacheControl = CacheControl.maxAge(365, TimeUnit.DAYS)
                .cachePublic()
                .immutable();
        if (eTag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(eTag)
                    .cacheControl(cacheControl)
                    .build();
        }
        String gridFsId = mediaService.getMediaByStorageFileName(storageFileName);
        Resource resource = imageService.downloadImage(gridFsId);
        String contentType = imageService.getContentType(gridFsId)
                .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resource.getFilename() + "\"")
                .cacheControl(cacheControl)
                .eTag(eTag)
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(resource.contentLength())
                .body(resource);
    }
    // ── Download (batch) ──────────────────────────────────────────────────────

    @PostMapping("/batch/download")
    @Operation(summary = "Batch Download Images as ZIP", description = "Accepts a list of Media SQL IDs and returns a ZIP archive "
            + "containing all the requested images. Each file is named by its storageFileName. "
            + "Unavailable files are skipped with an error_id_<id>.txt marker in the ZIP.")
    public ResponseEntity<byte[]> batchDownloadImages(@RequestBody List<Long> ids) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            for (Long id : ids) {
                try {
                    ImageResponse media = mediaService.getMediaById(id);
                    Resource resource = imageService.downloadImage(media.getFilePath());

                    zip.putNextEntry(new ZipEntry(media.getStorageFileName()));
                    try (InputStream in = resource.getInputStream()) {
                        in.transferTo(zip);
                    }
                    zip.closeEntry();
                } catch (Exception e) {
                    // Skip unavailable files — add an error marker entry so caller knows
                    zip.putNextEntry(new ZipEntry("error_id_" + id + ".txt"));
                    zip.write(("Could not download id=" + id + ": " + e.getMessage()).getBytes());
                    zip.closeEntry();
                }
            }
        }

        byte[] zipBytes = baos.toByteArray();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"images_batch.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(zipBytes.length)
                .body(zipBytes);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @Operation(summary = "Update Image Metadata", description = "Updates mutable fields: altText, mediaType, isActive. "
            + "The storageFileName and public URL are not affected.")
    public ResponseEntity<ApiResponse<ImageResponse>> updateImage(
            @PathVariable Long id,
            @Valid @RequestBody MediaRequestDto requestDto) {
        ImageResponse response = mediaService.updateMedia(id, requestDto);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_UPDATED));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Image", description = "Permanently removes from GridFS and soft-deletes the SQL Media record")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable Long id) {
        mediaService.deleteMediaWithStorage(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, AppConstants.MSG_DELETED));
    }

    @DeleteMapping("/batch")
    @Operation(summary = "Batch Delete Images", description = "Deletes multiple images from GridFS and soft-deletes their SQL records")
    public ResponseEntity<ApiResponse<Void>> deleteImages(@RequestBody List<Long> ids) {
        mediaService.deleteMediaListWithStorage(ids);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, AppConstants.MSG_DELETED));
    }
}
