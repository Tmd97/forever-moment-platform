package com.forvmom.core.services;

import com.forvmom.core.config.ImageUrlConfig;
import com.forvmom.data.dao.MediaDao;
import com.forvmom.data.dao.MediaVariantDao;
import com.forvmom.data.entities.Media;
import com.forvmom.data.entities.MediaVariant;
import com.forvmom.data.entities.MediaVariantType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ImageVariantService {

    public static class VariantPayload {
        private final MediaVariantType type;
        private final String storageFileName;
        private final String filePath;
        private final String mimeType;
        private final long fileSizeBytes;
        private final Integer widthPx;
        private final Integer heightPx;

        public VariantPayload(MediaVariantType type, String storageFileName, String filePath, String mimeType,
                long fileSizeBytes, Integer widthPx, Integer heightPx) {
            this.type = type;
            this.storageFileName = storageFileName;
            this.filePath = filePath;
            this.mimeType = mimeType;
            this.fileSizeBytes = fileSizeBytes;
            this.widthPx = widthPx;
            this.heightPx = heightPx;
        }
    }

    public static class VariantUrls {
        private String originalUrl;
        private String heroUrl;
        private String thumbnailUrl;

        public String getOriginalUrl() {
            return originalUrl;
        }

        public String getHeroUrl() {
            return heroUrl;
        }

        public String getThumbnailUrl() {
            return thumbnailUrl;
        }
    }

    private final MediaVariantDao mediaVariantDao;
    private final MediaDao mediaDao;
    private final ImageUrlConfig imageUrlConfig;
    private final ImageFlowCacheService imageFlowCacheService;

    public ImageVariantService(MediaVariantDao mediaVariantDao,
            MediaDao mediaDao,
            ImageUrlConfig imageUrlConfig,
            ImageFlowCacheService imageFlowCacheService) {
        this.mediaVariantDao = mediaVariantDao;
        this.mediaDao = mediaDao;
        this.imageUrlConfig = imageUrlConfig;
        this.imageFlowCacheService = imageFlowCacheService;
    }

    @Transactional
    public void saveOrUpdateVariant(Long mediaId, VariantPayload payload) {
        MediaVariant existing = mediaVariantDao.findByMediaIdAndVariantType(mediaId, payload.type);
        MediaVariant entity = existing != null ? existing : new MediaVariant();

        if (existing == null) {
            Media mediaRef = mediaDao.findById(mediaId);
            if (mediaRef == null) {
                return;
            }
            entity.setMedia(mediaRef);
            entity.setVariantType(payload.type);
        } else {
            imageFlowCacheService.evictResolvedFilePath(existing.getStorageFileName());
        }

        entity.setStorageFileName(payload.storageFileName);
        entity.setFilePath(payload.filePath);
        entity.setMimeType(payload.mimeType);
        entity.setFileSizeBytes(payload.fileSizeBytes);
        entity.setWidthPx(payload.widthPx);
        entity.setHeightPx(payload.heightPx);

        if (existing == null) {
            mediaVariantDao.save(entity);
        } else {
            mediaVariantDao.update(entity);
        }

        imageFlowCacheService.putResolvedFilePath(payload.storageFileName, payload.filePath);
    }

    @Transactional(readOnly = true)
    public VariantUrls getUrlsForMedia(Long mediaId) {
        Map<Long, VariantUrls> map = getUrlsForMediaIds(Collections.singletonList(mediaId));
        return map.get(mediaId);
    }

    @Transactional(readOnly = true)
    public Map<Long, VariantUrls> getUrlsForMediaIds(List<Long> mediaIds) {
        Map<Long, VariantUrls> result = new HashMap<>();
        if (mediaIds == null || mediaIds.isEmpty()) {
            return result;
        }

        for (MediaVariant variant : mediaVariantDao.findByMediaIds(mediaIds)) {
            Long id = variant.getMedia().getId();
            VariantUrls urls = result.computeIfAbsent(id, ignored -> new VariantUrls());
            String url = imageUrlConfig.buildPublicUrl(variant.getStorageFileName());
            if (variant.getVariantType() == MediaVariantType.ORIGINAL) {
                urls.originalUrl = url;
            } else if (variant.getVariantType() == MediaVariantType.HERO) {
                urls.heroUrl = url;
            } else if (variant.getVariantType() == MediaVariantType.THUMB) {
                urls.thumbnailUrl = url;
            }
        }

        // Fallback chain: hero -> original, thumb -> hero/original
        for (VariantUrls urls : result.values()) {
            if (urls.heroUrl == null) {
                urls.heroUrl = urls.originalUrl;
            }
            if (urls.thumbnailUrl == null) {
                urls.thumbnailUrl = urls.heroUrl != null ? urls.heroUrl : urls.originalUrl;
            }
        }

        return result;
    }

    @Transactional
    public void deleteVariantsByMediaId(Long mediaId) {
        for (MediaVariant variant : mediaVariantDao.findByMediaId(mediaId)) {
            imageFlowCacheService.evictResolvedFilePath(variant.getStorageFileName());
        }
        mediaVariantDao.deleteByMediaId(mediaId);
    }
}

