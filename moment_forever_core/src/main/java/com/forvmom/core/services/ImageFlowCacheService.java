package com.forvmom.core.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forvmom.common.dto.response.AddonResponseDto;
import com.forvmom.common.dto.response.ExperienceAddonResponseDto;
import com.forvmom.common.dto.response.ExperienceHighlightResponseDto;
import com.forvmom.common.dto.response.ExperienceResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis cache for image-related read flows:
 * - Experience detail payload with media URLs
 * - Experience list payloads with card image URLs
 * - storageFileName -> object-store path resolution
 */
@Service
public class ImageFlowCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ImageFlowCacheService.class);

    private static final long EXPERIENCE_DETAIL_TTL_MINUTES = 10;
    private static final long EXPERIENCE_LIST_TTL_MINUTES = 10;
    private static final long IMAGE_RESOLVE_TTL_HOURS = 2;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public ImageFlowCacheService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public ExperienceResponseDto getExperienceDetail(Long experienceId) {
        String json = redis.opsForValue().get(experienceDetailKey(experienceId));
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ExperienceResponseDto.class);
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize experience detail cache for experienceId={}", experienceId, e);
            return null;
        }
    }

    public void putExperienceDetail(Long experienceId, ExperienceResponseDto response) {
        try {
            redis.opsForValue().set(
                    experienceDetailKey(experienceId),
                    objectMapper.writeValueAsString(response),
                    EXPERIENCE_DETAIL_TTL_MINUTES,
                    TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize experience detail cache for experienceId={}", experienceId, e);
        }
    }

    public void evictExperienceDetail(Long experienceId) {
        redis.delete(experienceDetailKey(experienceId));
    }

    public List<ExperienceHighlightResponseDto> getExperienceListAll() {
        return getExperienceListPayload(experienceListAllNoPaginationKey());
    }

    public void putExperienceListAll(List<ExperienceHighlightResponseDto> response) {
        putExperienceListPayload(experienceListAllNoPaginationKey(), response);
    }

    public List<ExperienceHighlightResponseDto> getExperienceListActive() {
        return getExperienceListPayload(experienceListActiveNoPaginationKey());
    }

    public void putExperienceListActive(List<ExperienceHighlightResponseDto> response) {
        putExperienceListPayload(experienceListActiveNoPaginationKey(), response);
    }

    public List<ExperienceHighlightResponseDto> getExperienceListFeatured() {
        return getExperienceListPayload(experienceListFeaturedNoPaginationKey());
    }

    public void putExperienceListFeatured(List<ExperienceHighlightResponseDto> response) {
        putExperienceListPayload(experienceListFeaturedNoPaginationKey(), response);
    }

    public List<ExperienceHighlightResponseDto> getExperienceListBySubCategory(Long subCategoryId) {
        return getExperienceListPayload(experienceListSubCategoryNoPaginationKey(subCategoryId));
    }

    public void putExperienceListBySubCategory(Long subCategoryId, List<ExperienceHighlightResponseDto> response) {
        putExperienceListPayload(experienceListSubCategoryNoPaginationKey(subCategoryId), response);
    }

    public List<ExperienceHighlightResponseDto> getExperienceListActivePage(int page, int size) {
        return getExperienceListPayload(experienceListActivePageKey(page, size));
    }

    public void putExperienceListActivePage(int page, int size, List<ExperienceHighlightResponseDto> response) {
        putExperienceListPayload(experienceListActivePageKey(page, size), response);
    }

    public List<ExperienceHighlightResponseDto> getExperienceListFeaturedPage(int page, int size) {
        return getExperienceListPayload(experienceListFeaturedPageKey(page, size));
    }

    public void putExperienceListFeaturedPage(int page, int size, List<ExperienceHighlightResponseDto> response) {
        putExperienceListPayload(experienceListFeaturedPageKey(page, size), response);
    }

    public List<ExperienceHighlightResponseDto> getExperienceListBySubCategoryPage(Long subCategoryId, int page, int size) {
        return getExperienceListPayload(experienceListSubCategoryPageKey(subCategoryId, page, size));
    }

    public void putExperienceListBySubCategoryPage(Long subCategoryId, int page, int size,
            List<ExperienceHighlightResponseDto> response) {
        putExperienceListPayload(experienceListSubCategoryPageKey(subCategoryId, page, size), response);
    }

    public void evictExperienceLists() {
        Set<String> keys = redis.keys("exp:list:*");
        if (keys == null || keys.isEmpty()) {
            logger.info("List cache eviction: no keys matched pattern exp:list:*");
            return;
        }
        redis.delete(keys);
        logger.info("List cache eviction: removed {} key(s) for pattern exp:list:*", keys.size());
    }

    public String getResolvedFilePath(String storageFileName) {
        return redis.opsForValue().get(imageResolveKey(storageFileName));
    }

    public void putResolvedFilePath(String storageFileName, String filePath) {
        redis.opsForValue().set(
                imageResolveKey(storageFileName),
                filePath,
                IMAGE_RESOLVE_TTL_HOURS,
                TimeUnit.HOURS);
    }

    public void evictResolvedFilePath(String storageFileName) {
        redis.delete(imageResolveKey(storageFileName));
    }

    public List<AddonResponseDto> getAddonMasterList() {
        String json = redis.opsForValue().get(addonMasterListKey());
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<AddonResponseDto>>() {
            });
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize addon master list cache for key={}", addonMasterListKey(), e);
            return null;
        }
    }

    public void putAddonMasterList(List<AddonResponseDto> response) {
        putPayload(addonMasterListKey(), response, EXPERIENCE_LIST_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public List<ExperienceAddonResponseDto> getAddonsByExperience(Long experienceId) {
        String key = addonsByExperienceKey(experienceId);
        String json = redis.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ExperienceAddonResponseDto>>() {
            });
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize experience addon list cache for key={}", key, e);
            return null;
        }
    }

    public void putAddonsByExperience(Long experienceId, List<ExperienceAddonResponseDto> response) {
        putPayload(addonsByExperienceKey(experienceId), response, EXPERIENCE_LIST_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public void evictAddonMasterList() {
        redis.delete(addonMasterListKey());
    }

    public void evictAddonsByExperience(Long experienceId) {
        redis.delete(addonsByExperienceKey(experienceId));
    }

    public void evictAllExperienceAddonLists() {
        Set<String> keys = redis.keys("exp:addons:*");
        if (keys == null || keys.isEmpty()) {
            logger.info("Experience addon list eviction: no keys matched pattern exp:addons:*");
            return;
        }
        redis.delete(keys);
        logger.info("Experience addon list eviction: removed {} key(s) for pattern exp:addons:*", keys.size());
    }

    private String experienceDetailKey(Long experienceId) {
        return "exp:detail:" + experienceId + ":v2";
    }

    private List<ExperienceHighlightResponseDto> getExperienceListPayload(String key) {
        String json = redis.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ExperienceHighlightResponseDto>>() {
            });
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize experience list cache for key={}", key, e);
            return null;
        }
    }

    private void putExperienceListPayload(String key, List<ExperienceHighlightResponseDto> response) {
        if (response == null) {
            return;
        }
        putPayload(key, response, EXPERIENCE_LIST_TTL_MINUTES, TimeUnit.MINUTES);
    }

    private String experienceListAllNoPaginationKey() {
        return "exp:list:all:v2:all";
    }

    private String experienceListActiveNoPaginationKey() {
        return "exp:list:active:v2:all";
    }

    private String experienceListFeaturedNoPaginationKey() {
        return "exp:list:featured:v2:all";
    }

    private String experienceListSubCategoryNoPaginationKey(Long subCategoryId) {
        if (subCategoryId == null) {
            return "exp:list:subcategory:none:v2:all";
        }
        return "exp:list:subcategory:" + subCategoryId + ":v2:all";
    }

    private String experienceListActivePageKey(int page, int size) {
        return "exp:list:active:v2:page:" + page + ":size:" + size;
    }

    private String experienceListFeaturedPageKey(int page, int size) {
        return "exp:list:featured:v2:page:" + page + ":size:" + size;
    }

    private String experienceListSubCategoryPageKey(Long subCategoryId, int page, int size) {
        if (subCategoryId == null) {
            return "exp:list:subcategory:none:v2:page:" + page + ":size:" + size;
        }
        return "exp:list:subcategory:" + subCategoryId + ":v2:page:" + page + ":size:" + size;
    }

    private String imageResolveKey(String storageFileName) {
        return "img:resolve:" + storageFileName;
    }

    private String addonMasterListKey() {
        return "addon:list:all:v1:all";
    }

    private String addonsByExperienceKey(Long experienceId) {
        if (experienceId == null) {
            return "exp:addons:none:v1:all";
        }
        return "exp:addons:" + experienceId + ":v1:all";
    }

    private void putPayload(String key, Object response, long ttl, TimeUnit timeUnit) {
        if (response == null) {
            return;
        }
        try {
            redis.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(response),
                    ttl,
                    timeUnit);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize cache payload for key={}", key, e);
        }
    }
}
