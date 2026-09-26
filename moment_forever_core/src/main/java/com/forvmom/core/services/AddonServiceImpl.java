package com.forvmom.core.services;

import com.forvmom.common.dto.request.AddonRequestDto;
import com.forvmom.common.dto.request.BulkAttachAddonRequestDto;
import com.forvmom.common.dto.request.BulkAttachAddonRequestDto.AddonAttachItem;
import com.forvmom.common.dto.response.AddonResponseDto;
import com.forvmom.common.dto.response.BulkAttachAddonResultDto;
import com.forvmom.common.dto.response.BulkAttachAddonResultDto.SkippedAddonDto;
import com.forvmom.common.dto.response.BulkAttachAddonResultDto.SkippedAddonDto.Reason;
import com.forvmom.common.dto.response.ExperienceAddonResponseDto;
import com.forvmom.common.errorhandler.ResourceNotFoundException;
import com.forvmom.core.mapper.AddonBeanMapper;
import com.forvmom.data.dao.AddonDao;
import com.forvmom.data.dao.ExperienceAddonMapperDao;
import com.forvmom.data.dao.ExperienceDao;
import com.forvmom.data.dao.MediaDao;
import com.forvmom.data.entities.Addon;
import com.forvmom.data.entities.Experience;
import com.forvmom.data.entities.ExperienceAddonMapper;
import com.forvmom.data.entities.Media;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AddonServiceImpl implements AddonService {

    private static final Logger logger = LoggerFactory.getLogger(AddonServiceImpl.class);

    @Autowired
    private AddonDao addonDao;

    @Autowired
    private ExperienceAddonMapperDao addonMapperDao;

    @Autowired
    private ExperienceDao experienceDao;

    @Autowired
    private CatalogCacheService catalogCacheService;

    @Autowired
    private MediaDao mediaDao;

    @Autowired
    private ImageVariantService imageVariantService;

    @Autowired
    private ImageFlowCacheService imageFlowCacheService;

    // ── Master Addon CRUD ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public AddonResponseDto createAddon(AddonRequestDto requestDto) {
        Addon addon = AddonBeanMapper.mapRequestToAddon(requestDto);
        addon.setImageMedia(resolveAddonMedia(requestDto.getMediaId()));
        AddonResponseDto response = AddonBeanMapper.mapAddonToDto(addonDao.save(addon));
        hydrateAddonImageUrls(Collections.singletonList(response));
        imageFlowCacheService.evictAddonMasterList();
        imageFlowCacheService.evictAllExperienceAddonLists();
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddonResponseDto> getAllAddons() {
        List<AddonResponseDto> cached = imageFlowCacheService.getAddonMasterList();
        if (cached != null) {
            logger.info("Cache hit for addon master list");
            return cached;
        }

        logger.info("Cache miss for addon master list; loading from DB");
        List<AddonResponseDto> response = addonDao.findAll().stream()
                .map(AddonBeanMapper::mapAddonToDto)
                .collect(Collectors.toList());
        hydrateAddonImageUrls(response);
        imageFlowCacheService.putAddonMasterList(response);
        return response;
    }

    @Override
    @Transactional
    public AddonResponseDto updateAddon(Long id, AddonRequestDto requestDto) {
        Addon existing = addonDao.findById(id);
        if (existing == null)
            throw new ResourceNotFoundException("Addon not found: " + id);
        AddonBeanMapper.updateAddonFromRequest(existing, requestDto);
        existing.setImageMedia(resolveAddonMedia(requestDto.getMediaId()));
        AddonResponseDto response = AddonBeanMapper.mapAddonToDto(addonDao.update(existing));
        hydrateAddonImageUrls(Collections.singletonList(response));
        imageFlowCacheService.evictAddonMasterList();
        imageFlowCacheService.evictAllExperienceAddonLists();
        return response;
    }

    @Override
    @Transactional
    public boolean deleteAddon(Long id) {
        Addon existing = addonDao.findById(id);
        if (existing == null)
            throw new ResourceNotFoundException("Addon not found: " + id);
        addonMapperDao.deleteAllByAddonId(id);
        addonDao.delete(existing);
        imageFlowCacheService.evictAddonMasterList();
        imageFlowCacheService.evictAllExperienceAddonLists();
        return true;
    }

    // ── Experience Attachment ─────────────────────────────────────────────────

    @Override
    @Transactional
    public ExperienceAddonResponseDto attachToExperience(Long experienceId, Long addonId,
            BigDecimal priceOverride, Boolean isFree) {
        if (addonMapperDao.existsByExperienceIdAndAddonId(experienceId, addonId)) {
            throw new IllegalStateException(
                    "Addon " + addonId + " is already attached to experience " + experienceId);
        }

        Experience experience = experienceDao.findById(experienceId);
        if (experience == null)
            throw new ResourceNotFoundException("Experience not found: " + experienceId);

        Addon addon = addonDao.findById(addonId);
        if (addon == null)
            throw new ResourceNotFoundException("Addon not found: " + addonId);

        ExperienceAddonMapper mapper = new ExperienceAddonMapper();
        mapper.setAddon(addon);
        mapper.setPriceOverride(priceOverride);
        mapper.setIsFree(Boolean.TRUE.equals(isFree));
        // Bidirectional helper — sets experience on mapper and adds to experience's set
        experience.addAddonMapper(mapper);

        ExperienceAddonMapper savedMapper = addonMapperDao.save(mapper);
        catalogCacheService.warmAddonCache(savedMapper);
        imageFlowCacheService.evictAddonsByExperience(experienceId);

        ExperienceAddonResponseDto response = AddonBeanMapper.mapAddonMapperToDto(savedMapper);
        hydrateExperienceAddonImageUrls(Collections.singletonList(response));
        return response;
    }

    /**
     * Bulk-attach addons to an experience. Skips (does not fail for) items that are
     * already attached or whose addon ID does not exist.
     */
    @Override
    @Transactional
    public BulkAttachAddonResultDto attachAddons(Long experienceId,
            BulkAttachAddonRequestDto requestDto) {
        List<ExperienceAddonResponseDto> attached = new ArrayList<>();
        List<SkippedAddonDto> skipped = new ArrayList<>();

        for (AddonAttachItem item : requestDto.getItems()) {
            Long addonId = item.getAddonId();
            try {
                ExperienceAddonResponseDto result = attachToExperience(
                        experienceId,
                        addonId,
                        item.getPriceOverride(),
                        item.getIsFree());
                attached.add(result);
            } catch (IllegalStateException e) {
                // Already attached
                skipped.add(new SkippedAddonDto(addonId, Reason.DUPLICATE, e.getMessage()));
            } catch (ResourceNotFoundException e) {
                // Addon or experience not found
                skipped.add(new SkippedAddonDto(addonId, Reason.NOT_FOUND, e.getMessage()));
            }
        }

        return new BulkAttachAddonResultDto(attached, skipped);
    }

    @Override
    @Transactional
    public void detachFromExperience(Long experienceId, Long addonId) {
        ExperienceAddonMapper mapper = addonMapperDao.findByExperienceIdAndAddonId(experienceId, addonId);
        if (mapper == null) {
            throw new ResourceNotFoundException(
                    "Addon " + addonId + " is not attached to experience " + experienceId);
        }

        Long mapperId = mapper.getId();
        addonMapperDao.delete(mapper);
        // Evict from cache
        catalogCacheService.evictAddon(mapperId);
        imageFlowCacheService.evictAddonsByExperience(experienceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExperienceAddonResponseDto> getAddonsForExperience(Long experienceId) {
        List<ExperienceAddonResponseDto> cached = imageFlowCacheService.getAddonsByExperience(experienceId);
        if (cached != null) {
            logger.info("Cache hit for experience addon list expId={}", experienceId);
            return cached;
        }

        logger.info("Cache miss for experience addon list expId={}; loading from DB", experienceId);
        List<ExperienceAddonMapper> mappers = addonMapperDao.findByExperienceId(experienceId);
        List<ExperienceAddonResponseDto> response = AddonBeanMapper.mapAddonMappers(mappers);
        hydrateExperienceAddonImageUrls(response);
        imageFlowCacheService.putAddonsByExperience(experienceId, response);
        return response;
    }

    private Media resolveAddonMedia(Long mediaId) {
        if (mediaId == null) {
            return null;
        }
        Media media = mediaDao.findById(mediaId);
        if (media == null) {
            throw new ResourceNotFoundException("Media not found: " + mediaId);
        }
        return media;
    }

    private void hydrateAddonImageUrls(List<AddonResponseDto> addons) {
        if (addons == null || addons.isEmpty()) {
            return;
        }

        List<Long> mediaIds = addons.stream()
                .map(AddonResponseDto::getMediaId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .collect(Collectors.toList());
        if (mediaIds.isEmpty()) {
            return;
        }

        Map<Long, ImageVariantService.VariantUrls> urlsByMediaId = imageVariantService.getUrlsForMediaIds(mediaIds);
        for (AddonResponseDto dto : addons) {
            if (dto.getMediaId() == null) {
                continue;
            }
            ImageVariantService.VariantUrls urls = urlsByMediaId.get(dto.getMediaId());
            if (urls != null) {
                dto.setHeroUrl(urls.getHeroUrl());
                dto.setThumbnailUrl(urls.getThumbnailUrl());
                dto.setOriginalUrl(urls.getOriginalUrl());
            }
        }
    }

    private void hydrateExperienceAddonImageUrls(List<ExperienceAddonResponseDto> addons) {
        if (addons == null || addons.isEmpty()) {
            return;
        }

        List<Long> mediaIds = addons.stream()
                .map(ExperienceAddonResponseDto::getMediaId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .collect(Collectors.toList());
        if (mediaIds.isEmpty()) {
            return;
        }

        Map<Long, ImageVariantService.VariantUrls> urlsByMediaId = imageVariantService.getUrlsForMediaIds(mediaIds);
        for (ExperienceAddonResponseDto dto : addons) {
            if (dto.getMediaId() == null) {
                continue;
            }
            ImageVariantService.VariantUrls urls = urlsByMediaId.get(dto.getMediaId());
            if (urls != null) {
                dto.setHeroUrl(urls.getHeroUrl());
                dto.setThumbnailUrl(urls.getThumbnailUrl());
                dto.setOriginalUrl(urls.getOriginalUrl());
            }
        }
    }
}
