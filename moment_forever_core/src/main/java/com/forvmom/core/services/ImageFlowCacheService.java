package com.forvmom.core.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forvmom.common.dto.response.ExperienceResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis cache for image-related read flows:
 * - Experience detail payload with media URLs
 * - storageFileName -> object-store path resolution
 */
@Service
public class ImageFlowCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ImageFlowCacheService.class);

    private static final long EXPERIENCE_DETAIL_TTL_MINUTES = 10;
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

    private String experienceDetailKey(Long experienceId) {
        return "exp:detail:" + experienceId + ":v2";
    }

    private String imageResolveKey(String storageFileName) {
        return "img:resolve:" + storageFileName;
    }
}

