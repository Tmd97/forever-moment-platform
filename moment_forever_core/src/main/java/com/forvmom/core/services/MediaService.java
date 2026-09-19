package com.forvmom.core.services;

import com.forvmom.common.dto.request.MediaRequestDto;
import com.forvmom.common.errorhandler.ResourceNotFoundException;
import com.forvmom.core.config.ImageUrlConfig;
import com.forvmom.core.mapper.MediaBeanMapper;
import com.forvmom.data.dao.ExperienceMediaMapperDao;
import com.forvmom.data.dao.MediaDao;
import com.forvmom.data.dao.MediaVariantDao;
import com.forvmom.data.entities.Media;
import com.forvmom.data.entities.MediaVariant;
import com.forvmom.store.api.ObjectStorageService;
import com.forvmom.store.dto.ImageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MediaService {

    @Autowired
    private MediaDao mediaDao;

    @Autowired
    private ImageUrlConfig imageUrlConfig;

    @Autowired
    private ObjectStorageService storageService;

    @Autowired
    private ImageFlowCacheService imageFlowCacheService;

    @Autowired
    private ExperienceMediaMapperDao experienceMediaMapperDao;

    @Autowired
    private ImageVariantService imageVariantService;

    @Autowired
    private MediaVariantDao mediaVariantDao;

    /**
     * Save media metadata to SQL database after file is uploaded to storage.
     * storageFileName = originalName_<timestamp> → URL is cache-busted by design.
     */
    @Transactional
    public ImageResponse saveMediaMetadata(String fileName,
            String storageFileName,
            String filePath,
            String contentType,
            long fileSize) {
        Media media = new Media();
        media.setFileName(fileName);
        media.setStorageFileName(storageFileName);
        media.setFilePath(filePath); // GridFS Id
        media.setMimeType(contentType);
        media.setFileSizeBytes(fileSize);
        // Derive mediaType from mime type (NOT NULL in DB)
        if (contentType != null && contentType.startsWith("video/")) {
            media.setMediaType("VIDEO");
        } else if (contentType != null && contentType.startsWith("image/")) {
            media.setMediaType("IMAGE");
        } else {
            media.setMediaType("FILE");
        }

        Media savedMedia = mediaDao.save(media);
        imageFlowCacheService.putResolvedFilePath(storageFileName, filePath);
        return MediaBeanMapper.mapEntityToDto(savedMedia, imageUrlConfig);
    }

    @Transactional(readOnly = true)
    public ImageResponse getMediaById(Long id) {
        ImageResponse dto = MediaBeanMapper.mapEntityToDto(findMediaById(id), imageUrlConfig);
        hydrateVariantUrls(dto);
        return dto;
    }

    @Transactional(readOnly = true)
    public String getMediaByStorageFileName(String storageFileName) {
        String cachedPath = imageFlowCacheService.getResolvedFilePath(storageFileName);
        if (cachedPath != null) {
            return cachedPath;
        }

        String filePath = mediaDao.findGridFsIdByStorageFileName(storageFileName);
        if (filePath == null) {
            filePath = mediaVariantDao.findFilePathByStorageFileName(storageFileName);
        }
        if (filePath == null) {
            throw new ResourceNotFoundException("Media not found with storage file name: " + storageFileName);
        }

        imageFlowCacheService.putResolvedFilePath(storageFileName, filePath);
        return filePath;
    }

    @Transactional(readOnly = true)
    public List<ImageResponse> getAllMedia() {
        return mediaDao.findAllActive().stream()
                .map(m -> {
                    ImageResponse dto = MediaBeanMapper.mapEntityToDto(m, imageUrlConfig);
                    hydrateVariantUrls(dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public String getGridFsIdByStorageFileName(String storageFileName) {
        return mediaDao.findGridFsIdByStorageFileName(storageFileName);
    }

    @Transactional(readOnly = true)
    public ImageResponse findByFilePath(String filePath) {
        Media media = mediaDao.findByFilePath(filePath);
        if (media == null) {
            return null;
        }
        ImageResponse dto = MediaBeanMapper.mapEntityToDto(media, imageUrlConfig);
        hydrateVariantUrls(dto);
        return dto;
    }

    // ── Update ───────────────────────────────────────────────────────────────

    /**
     * Partial update of mutable metadata fields (altText, mediaType, isActive).
     * The storageFileName / filePath (and therefore the public URL) remain
     * unchanged.
     */
    @Transactional
    public ImageResponse updateMedia(Long id, MediaRequestDto dto) {
        Media media = findMediaById(id);
        if (dto.getAltText() != null)
            media.setAltText(dto.getAltText());
        if (dto.getMediaType() != null)
            media.setMediaType(dto.getMediaType());
        if (dto.getIsActive() != null)
            media.setIsActive(dto.getIsActive());
        Media updated = mediaDao.update(media);
        imageFlowCacheService.putResolvedFilePath(updated.getStorageFileName(), updated.getFilePath());
        evictExperienceDetailsForMedia(updated.getId());
        ImageResponse response = MediaBeanMapper.mapEntityToDto(updated, imageUrlConfig);
        hydrateVariantUrls(response);
        return response;
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    /**
     * Delete from GridFS first, then soft-delete the SQL row.
     * Uses filePath (GridFS Id) stored on the entity.
     */
    @Transactional
    public void deleteMediaWithStorage(Long id) {
        Media media = findMediaById(id);
        evictExperienceDetailsForMedia(media.getId());
        for (MediaVariant variant : mediaVariantDao.findByMediaId(media.getId())) {
            if (!media.getFilePath().equals(variant.getFilePath())) {
                storageService.delete(variant.getFilePath());
            }
        }
        imageVariantService.deleteVariantsByMediaId(media.getId());
        storageService.delete(media.getFilePath());
        mediaDao.delete(media);
        imageFlowCacheService.evictResolvedFilePath(media.getStorageFileName());
    }

    /**
     * Batch delete — removes each from GridFS then soft-deletes SQL rows.
     */
    @Transactional
    public void deleteMediaListWithStorage(List<Long> ids) {
        for (Long id : ids) {
            deleteMediaWithStorage(id);
        }
    }

    private Media findMediaById(Long id) {
        Media media = mediaDao.findById(id);
        if (media == null) {
            throw new ResourceNotFoundException("Media not found with id: " + id);
        }
        return media;
    }

    private void evictExperienceDetailsForMedia(Long mediaId) {
        for (Long experienceId : experienceMediaMapperDao.findExperienceIdsByMediaId(mediaId)) {
            imageFlowCacheService.evictExperienceDetail(experienceId);
        }
    }

    public void hydrateVariantUrls(ImageResponse dto) {
        if (dto == null || dto.getId() == null) {
            return;
        }

        // get all VariantUrls object for the given media id (from the table
        // relationship)
        ImageVariantService.VariantUrls urls = imageVariantService.getUrlsForMedia(dto.getId());
        if (urls == null) {
            return;
        }

        if (urls.getHeroUrl() != null) {
            dto.setMediaUrl(urls.getHeroUrl());
            dto.setUrl(urls.getHeroUrl());
        }
        if (urls.getThumbnailUrl() != null) {
            dto.setThumbnailUrl(urls.getThumbnailUrl());
        }
        if (urls.getOriginalUrl() != null) {
            dto.setOriginalUrl(urls.getOriginalUrl());
        }
    }
}
