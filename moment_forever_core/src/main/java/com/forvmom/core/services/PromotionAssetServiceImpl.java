package com.forvmom.core.services;

import com.forvmom.common.dto.request.PromotionAssetRequestDto;
import com.forvmom.common.dto.response.PromotionImageResponseDto;
import com.forvmom.common.errorhandler.ResourceNotFoundException;
import com.forvmom.core.config.ImageUrlConfig;
import com.forvmom.data.dao.MediaDao;
import com.forvmom.data.dao.PromotionAssetDao;
import com.forvmom.data.entities.Media;
import com.forvmom.data.entities.PromotionAsset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PromotionAssetServiceImpl implements PromotionAssetService {

    @Autowired
    private PromotionAssetDao promotionAssetDao;

    @Autowired
    private MediaDao mediaDao;

    @Autowired
    private ImageVariantService imageVariantService;

    @Autowired
    private ImageUrlConfig imageUrlConfig;

    @Autowired
    private ImageFlowCacheService imageFlowCacheService;

    @Override
    @Transactional
    public PromotionImageResponseDto create(PromotionAssetRequestDto requestDto) {
        validateWindow(requestDto.getStartAt(), requestDto.getEndAt());
        PromotionAsset entity = new PromotionAsset();
        applyRequest(entity, requestDto);
        PromotionAsset saved = promotionAssetDao.save(entity);
        imageFlowCacheService.evictPromotionCaches();
        return mapToResponse(saved, null);
    }

    @Override
    @Transactional
    public PromotionImageResponseDto update(Long id, PromotionAssetRequestDto requestDto) {
        PromotionAsset existing = promotionAssetDao.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Promotion asset not found: " + id);
        }

        validateWindow(requestDto.getStartAt(), requestDto.getEndAt());
        applyRequest(existing, requestDto);
        PromotionAsset updated = promotionAssetDao.update(existing);
        imageFlowCacheService.evictPromotionCaches();
        return mapToResponse(updated, null);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        PromotionAsset existing = promotionAssetDao.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Promotion asset not found: " + id);
        }
        promotionAssetDao.delete(existing);
        imageFlowCacheService.evictPromotionCaches();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionImageResponseDto> listForAdmin(String key, String placement, Boolean isActive) {
        List<PromotionAsset> assets = promotionAssetDao.findByFilters(normalizeKey(key), normalizePlacement(placement), isActive);
        return hydrateAndMap(assets);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionImageResponseDto> listForPublic(String key, String placement) {
        String normalizedKey = normalizeKeyRequired(key);
        String normalizedPlacement = normalizePlacement(placement);
        List<PromotionImageResponseDto> cached = imageFlowCacheService.getPromotionList(normalizedKey, normalizedPlacement);
        if (cached != null) {
            return cached;
        }

        List<PromotionAsset> assets = promotionAssetDao.findActiveByKeyAndPlacement(
                normalizedKey,
                normalizedPlacement,
                LocalDateTime.now());
        List<PromotionImageResponseDto> mapped = hydrateAndMap(assets);
        imageFlowCacheService.putPromotionList(normalizedKey, normalizedPlacement, mapped);
        return mapped;
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionImageResponseDto getSingleForPublic(String key, String placement) {
        String normalizedKey = normalizeKeyRequired(key);
        String normalizedPlacement = normalizePlacement(placement);
        PromotionImageResponseDto cached = imageFlowCacheService.getPromotionSingle(normalizedKey, normalizedPlacement);
        if (cached != null) {
            return cached;
        }

        List<PromotionAsset> assets = promotionAssetDao.findActiveByKeyAndPlacement(
                normalizedKey,
                normalizedPlacement,
                LocalDateTime.now());
        if (assets.isEmpty()) {
            return null;
        }

        PromotionImageResponseDto first = hydrateAndMap(assets).stream().findFirst().orElse(null);
        if (first != null) {
            imageFlowCacheService.putPromotionSingle(normalizedKey, normalizedPlacement, first);
        }
        return first;
    }

    private void applyRequest(PromotionAsset entity, PromotionAssetRequestDto requestDto) {
        entity.setMedia(resolveMedia(requestDto.getMediaId()));
        entity.setPromoKey(normalizeKeyRequired(requestDto.getPromoKey()));
        entity.setPlacement(normalizePlacementRequired(requestDto.getPlacement()));
        entity.setStartAt(requestDto.getStartAt());
        entity.setEndAt(requestDto.getEndAt());
        entity.setPriority(requestDto.getPriority() == null ? 100 : requestDto.getPriority());
        entity.setIsActive(requestDto.getIsActive() == null ? Boolean.TRUE : requestDto.getIsActive());
        entity.setTitle(requestDto.getTitle());
        entity.setAltTextOverride(requestDto.getAltTextOverride());
    }

    private Media resolveMedia(Long mediaId) {
        Media media = mediaDao.findById(mediaId);
        if (media == null) {
            throw new ResourceNotFoundException("Media not found: " + mediaId);
        }
        if (!Boolean.TRUE.equals(media.getIsActive())) {
            throw new IllegalStateException("Media is inactive: " + mediaId);
        }
        return media;
    }

    private void validateWindow(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
            throw new IllegalArgumentException("endAt must be greater than or equal to startAt");
        }
    }

    private List<PromotionImageResponseDto> hydrateAndMap(List<PromotionAsset> assets) {
        if (assets == null || assets.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> mediaIds = assets.stream()
                .map(a -> a.getMedia().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, ImageVariantService.VariantUrls> urlMap = imageVariantService.getUrlsForMediaIds(new ArrayList<>(mediaIds));

        return assets.stream()
                .map(asset -> mapToResponse(asset, urlMap.get(asset.getMedia().getId())))
                .collect(Collectors.toList());
    }

    private PromotionImageResponseDto mapToResponse(PromotionAsset asset, ImageVariantService.VariantUrls urls) {
        PromotionImageResponseDto dto = new PromotionImageResponseDto();
        dto.setId(asset.getId());
        dto.setMediaId(asset.getMedia().getId());
        dto.setPromoKey(asset.getPromoKey());
        dto.setPlacement(asset.getPlacement());
        dto.setStartAt(asset.getStartAt());
        dto.setEndAt(asset.getEndAt());
        dto.setPriority(asset.getPriority());
        dto.setIsActive(asset.getIsActive());
        dto.setTitle(asset.getTitle());
        dto.setAltTextOverride(asset.getAltTextOverride());
        dto.setFileName(asset.getMedia().getFileName());
        dto.setStorageFileName(asset.getMedia().getStorageFileName());

        if (urls != null) {
            dto.setHeroUrl(urls.getHeroUrl());
            dto.setThumbnailUrl(urls.getThumbnailUrl());
            dto.setOriginalUrl(urls.getOriginalUrl());
        }
        if (dto.getHeroUrl() == null) {
            dto.setHeroUrl(imageUrlConfig.buildPublicUrl(asset.getMedia().getStorageFileName()));
        }
        if (dto.getOriginalUrl() == null) {
            dto.setOriginalUrl(imageUrlConfig.buildPublicUrl(asset.getMedia().getStorageFileName()));
        }
        dto.setUrl(dto.getHeroUrl());
        return dto;
    }

    private String normalizeKey(String input) {
        return input == null ? null : input.trim().toLowerCase();
    }

    private String normalizePlacement(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        return input.trim().toLowerCase();
    }

    private String normalizeKeyRequired(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Promotion key is required");
        }
        return input.trim().toLowerCase();
    }

    private String normalizePlacementRequired(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Placement is required");
        }
        return input.trim().toLowerCase();
    }
}
