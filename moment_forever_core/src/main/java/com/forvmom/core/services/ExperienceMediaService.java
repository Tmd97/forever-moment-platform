package com.forvmom.core.services;

import com.forvmom.common.dto.request.BulkAttachMediaRequestDto;
import com.forvmom.common.dto.request.ExperienceMediaAttachRequestDto;
import com.forvmom.common.dto.response.BulkAttachMediaResultDto;
import com.forvmom.common.dto.response.ExperienceMediaResponseDto;
import com.forvmom.common.errorhandler.ResourceNotFoundException;
import com.forvmom.core.config.ImageUrlConfig;
import com.forvmom.core.mapper.ExperienceMediaBeanMapper;
import com.forvmom.data.dao.ExperienceMediaMapperDao;
import com.forvmom.data.dao.MediaDao;
import com.forvmom.data.entities.Experience;
import com.forvmom.data.entities.ExperienceMediaMapper;
import com.forvmom.data.entities.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ExperienceMediaService {

    @Autowired
    private ExperienceMediaMapperDao experienceMediaMapperDao;

    @Autowired
    private MediaDao mediaDao;

    @Autowired
    private ImageUrlConfig imageUrlConfig;

    @Autowired
    private ImageFlowCacheService imageFlowCacheService;

    @Autowired
    private ImageVariantService imageVariantService;

    // We resolve experience via ExperienceDao (reuse existing)
    @Autowired
    private com.forvmom.data.dao.ExperienceDao experienceDao;

    // ── Attach ────────────────────────────────────────────────────────────────

    /**
     * Attaches a Media record to an Experience.
     * If isPrimary is true, demotes any existing primary mapper first.
     */
    @Transactional
    public ExperienceMediaResponseDto attachMedia(Long experienceId, Long mediaId,
            ExperienceMediaAttachRequestDto requestDto) {
        Experience experience = findExperienceOrThrow(experienceId);
        Media media = findMediaOrThrow(mediaId);

        if (experienceMediaMapperDao.existsByExperienceIdAndMediaId(experienceId, mediaId)) {
            throw new IllegalStateException(
                    "Media " + mediaId + " is already attached to experience " + experienceId + ".");
        }

        // Demote existing primary if this one is being set as primary
        if (Boolean.TRUE.equals(requestDto.getIsPrimary())) {
            demoteCurrentPrimary(experienceId);
        }

        ExperienceMediaMapper mapper = ExperienceMediaBeanMapper.mapDtoToEntity(requestDto);
        mapper.setMedia(media);
        experience.addMediaMapper(mapper);

        ExperienceMediaResponseDto response = ExperienceMediaBeanMapper.mapEntityToDto(
                experienceMediaMapperDao.save(mapper), imageUrlConfig);
        applyVariantUrls(Collections.singletonList(response));
        imageFlowCacheService.evictExperienceDetail(experienceId);
        imageFlowCacheService.evictExperienceLists();
        return response;
    }

    /**
     * Bulk-attach: attach multiple images to an experience in one request.
     * Duplicates and not-found IDs are skipped (reported in response) rather
     * than failing the whole operation.
     */
    @Transactional
    public BulkAttachMediaResultDto bulkAttachMedia(Long experienceId,
            BulkAttachMediaRequestDto requestDto) {
        List<ExperienceMediaResponseDto> attached = new ArrayList<>();
        List<BulkAttachMediaResultDto.SkippedMediaDto> skipped = new ArrayList<>();

        for (BulkAttachMediaRequestDto.MediaAttachItem item : requestDto.getItems()) {
            Long mediaId = item.getMediaId();
            try {
                attached.add(attachMedia(experienceId, mediaId, item));
            } catch (IllegalStateException e) {
                skipped.add(new BulkAttachMediaResultDto.SkippedMediaDto(
                        mediaId, BulkAttachMediaResultDto.SkippedMediaDto.Reason.DUPLICATE, e.getMessage()));
            } catch (ResourceNotFoundException e) {
                skipped.add(new BulkAttachMediaResultDto.SkippedMediaDto(
                        mediaId, BulkAttachMediaResultDto.SkippedMediaDto.Reason.NOT_FOUND, e.getMessage()));
            }
        }
        if (!attached.isEmpty()) {
            imageFlowCacheService.evictExperienceDetail(experienceId);
            imageFlowCacheService.evictExperienceLists();
        }
        return new BulkAttachMediaResultDto(attached, skipped);
    }

    @Transactional(readOnly = true)
    public List<ExperienceMediaResponseDto> getMediaForExperience(Long experienceId) {
        List<ExperienceMediaResponseDto> response = ExperienceMediaBeanMapper.mapEntitiesToDto(
                experienceMediaMapperDao.findByExperienceId(experienceId), imageUrlConfig);
        applyVariantUrls(response);
        return response;
    }

    @Transactional(readOnly = true)
    public ExperienceMediaResponseDto getPrimaryMedia(Long experienceId) {
        ExperienceMediaMapper mapper = experienceMediaMapperDao.findPrimaryByExperienceId(experienceId);
        if (mapper == null)
            throw new ResourceNotFoundException(
                    "No primary image set for experience " + experienceId + ".");
        ExperienceMediaResponseDto response = ExperienceMediaBeanMapper.mapEntityToDto(mapper, imageUrlConfig);
        applyVariantUrls(Collections.singletonList(response));
        return response;
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Update junction-row metadata: displayOrder, isPrimary, altText, isActive.
     * If isPrimary is flipped to true, demotes the existing primary first.
     */
    @Transactional
    public ExperienceMediaResponseDto updateAttachment(Long experienceId, Long mediaId,
            ExperienceMediaAttachRequestDto requestDto) {
        ExperienceMediaMapper mapper = findMapperOrThrow(experienceId, mediaId);

        if (Boolean.TRUE.equals(requestDto.getIsPrimary()) && !Boolean.TRUE.equals(mapper.getIsPrimary())) {
            demoteCurrentPrimary(experienceId);
        }

        ExperienceMediaBeanMapper.updateEntityFromDto(mapper, requestDto);
        ExperienceMediaResponseDto response = ExperienceMediaBeanMapper.mapEntityToDto(
                experienceMediaMapperDao.update(mapper), imageUrlConfig);
        applyVariantUrls(Collections.singletonList(response));
        imageFlowCacheService.evictExperienceDetail(experienceId);
        imageFlowCacheService.evictExperienceLists();
        return response;
    }

    // ── Detach ────────────────────────────────────────────────────────────────

    @Transactional
    public void detachMedia(Long experienceId, Long mediaId) {
        ExperienceMediaMapper mapper = findMapperOrThrow(experienceId, mediaId);
        experienceMediaMapperDao.delete(mapper);
        imageFlowCacheService.evictExperienceDetail(experienceId);
        imageFlowCacheService.evictExperienceLists();
    }

    @Transactional
    public void toggleAttachmentActive(Long mapperId) {
        ExperienceMediaMapper mapper = experienceMediaMapperDao.findById(mapperId);
        if (mapper == null)
            throw new ResourceNotFoundException("Media mapping not found: " + mapperId);
        mapper.setIsActive(!Boolean.TRUE.equals(mapper.getIsActive()));
        experienceMediaMapperDao.update(mapper);
        imageFlowCacheService.evictExperienceDetail(mapper.getExperience().getId());
        imageFlowCacheService.evictExperienceLists();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void demoteCurrentPrimary(Long experienceId) {
        ExperienceMediaMapper current = experienceMediaMapperDao.findPrimaryByExperienceId(experienceId);
        if (current != null) {
            current.setIsPrimary(false);
            experienceMediaMapperDao.update(current);
        }
    }

    private ExperienceMediaMapper findMapperOrThrow(Long experienceId, Long mediaId) {
        ExperienceMediaMapper mapper = experienceMediaMapperDao.findByExperienceIdAndMediaId(
                experienceId, mediaId);
        if (mapper == null)
            throw new ResourceNotFoundException(
                    "Media " + mediaId + " is not attached to experience " + experienceId + ".");
        return mapper;
    }

    private Experience findExperienceOrThrow(Long experienceId) {
        Experience exp = experienceDao.findById(experienceId);
        if (exp == null)
            throw new ResourceNotFoundException("Experience not found: " + experienceId);
        return exp;
    }

    private Media findMediaOrThrow(Long mediaId) {
        Media media = mediaDao.findById(mediaId);
        if (media == null)
            throw new ResourceNotFoundException("Media not found: " + mediaId);
        return media;
    }

    public void applyVariantUrls(List<ExperienceMediaResponseDto> mediaDtos) {
        if (mediaDtos == null || mediaDtos.isEmpty()) {
            return;
        }

        List<Long> mediaIds = mediaDtos.stream()
                .map(ExperienceMediaResponseDto::getMediaId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<Long, ImageVariantService.VariantUrls> urlsByMediaId = imageVariantService.getUrlsForMediaIds(mediaIds);
        for (ExperienceMediaResponseDto dto : mediaDtos) {
            if (dto.getMediaId() == null) {
                continue;
            }
            ImageVariantService.VariantUrls urls = urlsByMediaId.get(dto.getMediaId());
            if (urls == null) {
                continue;
            }
            if (urls.getHeroUrl() != null) {
                dto.setHeroUrl(urls.getHeroUrl());
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
}
