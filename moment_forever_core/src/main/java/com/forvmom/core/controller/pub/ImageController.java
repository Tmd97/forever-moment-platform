package com.forvmom.core.controller.pub;

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
 * Image upload, metadata and streaming endpoints.
 *
 * <p>
 * Mapped under {@code /public/images}. Binaries live in MongoDB GridFS while a
 * SQL {@code Media} row holds the metadata; the SQL row's {@code filePath} is
 * the GridFS id used for retrieval. Uploads derive a
 * {@code storageFileName} of the form {@code originalName_<timestamp>} so a
 * re-uploaded image gets a new URL and CDN/browser caches are busted.
 *
 * <p>
 * Downloads stream the stored {@link Resource} back with the content type
 * recorded in GridFS, falling back to {@code application/octet-stream}, and use
 * {@code Content-Disposition: inline} so browsers render rather than save the
 * image. The batch download endpoint instead assembles a ZIP in memory and is
 * served as an attachment.
 */
@RestController
@RequestMapping("/public/images")
@Tag(name = "Public Image API", description = "Upload and manage images (Public only)")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @Autowired
    private MediaService mediaService;

    // ── Upload (single) ───────────────────────────────────────────────────────

    /**
     * Uploads a single image to GridFS and creates its SQL media record.
     *
     * @param file     the multipart image file to store
     * @param metadata free-form request parameters stored as GridFS user metadata
     * @return {@code 201 CREATED} wrapping the created {@link ImageResponse}
     */
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

    /**
     * Uploads several images in one request, storing each one exactly as the single
     * upload endpoint does. The same metadata is applied to every file.
     *
     * @param files    the multipart image files to store
     * @param metadata free-form request parameters stored as GridFS user metadata
     * @return {@code 201 CREATED} wrapping one {@link ImageResponse} per file, in
     *         request order
     */
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

    /**
     * Lists all active media records with their public URLs.
     *
     * @return {@code 200 OK} wrapping the list of {@link ImageResponse}
     */
    @GetMapping
    @Operation(summary = "List All Images", description = "Returns all active media records with public URLs")
    public ResponseEntity<ApiResponse<List<ImageResponse>>> getAllImages() {
        List<ImageResponse> response = mediaService.getAllMedia();
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    /**
     * Returns the metadata of a single media record.
     *
     * @param id SQL identifier of the media record
     * @return {@code 200 OK} wrapping the {@link ImageResponse}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get Image by SQL ID")
    public ResponseEntity<ApiResponse<ImageResponse>> getImageById(@PathVariable Long id) {
        ImageResponse response = mediaService.getMediaById(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    // ── Download (single) ─────────────────────────────────────────────────────

    /**
     * Streams an image identified by its SQL media id.
     *
     * <p>
     * The GridFS id is read from the media record's {@code filePath}, and the
     * response is marked {@code inline} so the browser displays it.
     *
     * @param id SQL identifier of the media record
     * @return {@code 200 OK} with the image bytes as a {@link Resource}
     * @throws IOException if the resource length cannot be determined
     */
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

    /**
     * Streams an image addressed by its cache-busting {@code storageFileName}.
     *
     * <p>
     * This is the URL form embedded in catalog responses: because the name carries
     * an upload timestamp, a replaced image gets a different path and caches do not
     * serve the stale binary.
     *
     * @param storageFileName generated unique file name of the stored image
     * @return {@code 200 OK} with the image bytes as a {@link Resource}
     * @throws IOException if the resource length cannot be determined
     */
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

    /**
     * Bundles the requested images into a ZIP archive returned as an attachment.
     *
     * <p>
     * The archive is built in memory and each entry is named by its
     * {@code storageFileName}. An image that cannot be read does not fail the whole
     * request: an {@code error_id_<id>.txt} entry is written in its place.
     *
     * @param ids SQL identifiers of the media records to include
     * @return {@code 200 OK} with the ZIP archive bytes
     * @throws IOException if the ZIP stream cannot be written or closed
     */
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

    /**
     * Updates the mutable metadata of a media record ({@code altText},
     * {@code mediaType}, {@code isActive}). The stored binary, its
     * {@code storageFileName} and the public URL are left untouched.
     *
     * @param id         SQL identifier of the media record
     * @param requestDto the new metadata values
     * @return {@code 200 OK} wrapping the updated {@link ImageResponse}
     */
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

    /**
     * Removes an image: the binary is permanently deleted from GridFS and the SQL
     * media record is soft-deleted.
     *
     * @param id SQL identifier of the media record
     * @return {@code 200 OK} with an empty payload
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Image", description = "Permanently removes from GridFS and soft-deletes the SQL Media record")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable Long id) {
        mediaService.deleteMediaWithStorage(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, AppConstants.MSG_DELETED));
    }

    /**
     * Deletes several images at once, applying the same GridFS delete plus SQL
     * soft-delete to each id.
     *
     * @param ids SQL identifiers of the media records to delete
     * @return {@code 200 OK} with an empty payload
     */
    @DeleteMapping("/batch")
    @Operation(summary = "Batch Delete Images", description = "Deletes multiple images from GridFS and soft-deletes their SQL records")
    public ResponseEntity<ApiResponse<Void>> deleteImages(@RequestBody List<Long> ids) {
        mediaService.deleteMediaListWithStorage(ids);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, AppConstants.MSG_DELETED));
    }
}
