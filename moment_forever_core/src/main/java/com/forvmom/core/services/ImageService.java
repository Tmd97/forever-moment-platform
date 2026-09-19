package com.forvmom.core.services;

import com.forvmom.common.utils.FileExtension;
import com.forvmom.data.entities.MediaVariantType;
import com.forvmom.store.api.ObjectStorageService;
import com.forvmom.store.dto.ImageMetadataResponse;
import com.forvmom.store.dto.ImageResponse;
import com.forvmom.store.dto.ObjectMetadata;
import com.forvmom.store.exception.ImageNotFoundException;
import com.forvmom.store.exception.ImageStorageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImageService {

    @Autowired
    private MediaService mediaService;

    @Autowired
    private ImageVariantService imageVariantService;

    private final ObjectStorageService storageService;

    public ImageService(ObjectStorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Upload an image to storage
     */
    // TODO: currently, we are creating the new record in sql when the file comes
    // for upload, but same file with changed content will come up, instead of new
    // record, we can update the existing record with new content and metadata, this
    // will help us to avoid duplicate records in sql and also help us to bust cache
    // when same file with changed content comes up.
    public ImageResponse uploadImage(MultipartFile file, Map<String, Object> metadata) {
        try {
            // Prepare metadata
            Map<String, Object> finalMetadata = new HashMap<>();
            if (metadata != null) {
                finalMetadata.putAll(metadata);
            }

            String originalName = file.getOriginalFilename();
            String originalStorageFileName = FileExtension.generateTimestampName(originalName);
            byte[] sourceBytes = file.getBytes();

            // Store the file
            String id = storageService.store(
                    originalStorageFileName,
                    new ByteArrayInputStream(sourceBytes),
                    file.getContentType(),
                    finalMetadata);

            // Save metadata to SQL database (use unique name with timestamp help in bust
            // cache)
            ImageResponse mediaResponse = mediaService.saveMediaMetadata(
                    originalName,
                    originalStorageFileName,
                    id, // this is a grid fs id or object storage id.
                    file.getContentType(),
                    sourceBytes.length);

            saveVariants(mediaResponse, originalStorageFileName, id, file.getContentType(), sourceBytes, finalMetadata);
            mediaService.hydrateVariantUrls(mediaResponse);
            return mediaResponse;

        } catch (IOException e) {
            throw new ImageStorageException("Failed to upload image", e);
        }
    }

    /**
     * Download an image by ID
     */
    public Resource downloadImage(String id) {
        try {
            return storageService.retrieve(id);
        } catch (Exception e) {
            throw new ImageNotFoundException("Image not found with id: " + id);
        }
    }

    /**
     * Get image metadata by ID
     */
    public ImageMetadataResponse getImageMetadata(String id) {
        Optional<ObjectMetadata> metadata = storageService.getMetadata(id);

        if (metadata.isEmpty()) {
            throw new ImageNotFoundException("Image not found with id: " + id);
        }

        return mapToImageMetadataResponse(metadata.get());
    }

    /**
     * Get all images metadata
     */
    public List<ImageMetadataResponse> getAllImages() {
        return storageService.listAll()
                .stream()
                .map(this::mapToImageMetadataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Delete an image by ID
     */
    public void deleteImage(String id) {
        if (!storageService.exists(id)) {
            throw new ImageNotFoundException("Image not found with id: " + id);
        }
        storageService.delete(id);
    }

    public Optional<String> getContentType(String id) {
        return storageService.getMetadata(id)
                .map(ObjectMetadata::getContentType);
    }

    /**
     * Delete multiple images
     */
    public void deleteImages(List<String> ids) {
        storageService.deleteAll(ids);
    }

    private ImageMetadataResponse mapToImageMetadataResponse(ObjectMetadata metadata) {
        ImageMetadataResponse response = new ImageMetadataResponse();
        response.setId(metadata.getId());
        response.setFileName(metadata.getFileName());
        response.setContentType(metadata.getContentType());
        response.setSize(metadata.getSize());
        response.setUploadDate(metadata.getUploadDate());
        response.setUserMetadata(metadata.getUserMetadata());
        return response;
    }

    // this method saving all the image variants like hero, thumb etc
    private void saveVariants(ImageResponse mediaResponse,
            String originalStorageFileName,
            String originalPath,
            String originalContentType,
            byte[] sourceBytes,
            Map<String, Object> metadata) throws IOException {
        Long mediaId = mediaResponse.getId();

        imageVariantService.saveOrUpdateVariant(mediaId, new ImageVariantService.VariantPayload(
                MediaVariantType.ORIGINAL, // save original variant
                originalStorageFileName, // fileName with timestamp
                originalPath, // gridfs Id of that(orignal) variant
                originalContentType,
                sourceBytes.length,
                null,
                null));

        if (originalContentType == null || !originalContentType.startsWith("image/")) {
            return;
        }

        var decoded = ImageVariantProcessor.decode(sourceBytes);
        if (decoded == null) {
            return;
        }

        String extension = FileExtension.getExtensionWithoutDot(originalStorageFileName);
        ImageVariantProcessor.RenderedVariant hero = ImageVariantProcessor.resize(decoded, 1280, extension);
        String heroStorageName = variantStorageName(originalStorageFileName, "hero", hero.getFormat());
        String heroPath = storageService.store(heroStorageName,
                new ByteArrayInputStream(hero.getBytes()),
                hero.getMimeType(),
                metadata);

        imageVariantService.saveOrUpdateVariant(mediaId, new ImageVariantService.VariantPayload(
                MediaVariantType.HERO,
                heroStorageName,
                heroPath,
                hero.getMimeType(),
                hero.getBytes().length,
                hero.getWidth(),
                hero.getHeight()));

        ImageVariantProcessor.RenderedVariant thumb = ImageVariantProcessor.resize(decoded, 320, extension);
        String thumbStorageName = variantStorageName(originalStorageFileName, "thumb", thumb.getFormat());
        String thumbPath = storageService.store(thumbStorageName,
                new ByteArrayInputStream(thumb.getBytes()),
                thumb.getMimeType(),
                metadata);
        imageVariantService.saveOrUpdateVariant(mediaId, new ImageVariantService.VariantPayload(
                MediaVariantType.THUMB,
                thumbStorageName,
                thumbPath,
                thumb.getMimeType(),
                thumb.getBytes().length,
                thumb.getWidth(),
                thumb.getHeight()));
    }

    private String variantStorageName(String originalStorageFileName, String variantLabel, String outputFormat) {
        String baseName = FileExtension.getNameWithoutExtension(originalStorageFileName);
        String extension = "." + outputFormat.toLowerCase();
        return baseName + "_" + variantLabel + "_" + System.currentTimeMillis() + extension;
    }
}