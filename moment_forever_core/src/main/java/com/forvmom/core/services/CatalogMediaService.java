package com.forvmom.core.services;

import com.forvmom.common.dto.request.ExperienceMediaAttachRequestDto;
import com.forvmom.common.dto.response.CatalogMediaCarrier;
import com.forvmom.common.dto.response.CategoryResponseDto;
import com.forvmom.common.dto.response.ExperienceMediaResponseDto;
import com.forvmom.common.dto.response.SubCategoryResponseDto;
import com.forvmom.common.errorhandler.ResourceNotFoundException;
import com.forvmom.core.config.ImageUrlConfig;
import com.forvmom.core.mapper.CatalogMediaBeanMapper;
import com.forvmom.data.dao.*;
import com.forvmom.data.entities.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Attach/detach/update media on Category and SubCategory, mirroring
 * ExperienceMediaService's behavior (upload goes through ImageService,
 * this service only wires the resulting Media to the owning entity).
 *
 * Mapping and enrichment reuse ExperienceMediaResponseDto,
 * CatalogMediaBeanMapper and ImageVariantService rather than duplicating
 * that logic per entity type - see MediaAttachmentEntity / CatalogMediaCarrier.
 */
@Service
public class CatalogMediaService {

    @Autowired
    private CategoryDao categoryDao;

    @Autowired
    private SubCategoryDao subCategoryDao;

    @Autowired
    private MediaDao mediaDao;

    @Autowired
    private CategoryMediaMapperDao categoryMediaMapperDao;

    @Autowired
    private SubCategoryMediaMapperDao subCategoryMediaMapperDao;

    @Autowired
    private ImageVariantService imageVariantService;

    @Autowired
    private ImageUrlConfig imageUrlConfig;

    // ── Category ──────────────────────────────────────────────────────────────

    @Transactional
    public ExperienceMediaResponseDto attachMediaToCategory(Long categoryId, Long mediaId,
            ExperienceMediaAttachRequestDto requestDto) {
        Category category = findCategoryOrThrow(categoryId);
        Media media = findMediaOrThrow(mediaId);
        if (categoryMediaMapperDao.existsByCategoryIdAndMediaId(categoryId, mediaId)) {
            throw new IllegalStateException("Media " + mediaId + " is already attached to category " + categoryId + ".");
        }
        if (Boolean.TRUE.equals(requestDto.getIsPrimary())) {
            demoteCategoryPrimary(categoryId);
        }
        boolean shouldBePrimary = Boolean.TRUE.equals(requestDto.getIsPrimary())
                || categoryMediaMapperDao.findPrimaryByCategoryId(categoryId) == null;

        CategoryMediaMapper mapper = new CategoryMediaMapper();
        mapper.setMedia(media);
        mapper.setDisplayOrder(requestDto.getDisplayOrder() != null ? requestDto.getDisplayOrder() : 0);
        mapper.setIsPrimary(shouldBePrimary);
        mapper.setAltText(requestDto.getAltText());
        mapper.setIsActive(requestDto.getIsActive() == null ? Boolean.TRUE : requestDto.getIsActive());
        category.addMediaMapper(mapper);
        return toDto(categoryMediaMapperDao.save(mapper));
    }

    @Transactional
    public ExperienceMediaResponseDto updateCategoryAttachment(Long categoryId, Long mediaId,
            ExperienceMediaAttachRequestDto requestDto) {
        CategoryMediaMapper mapper = findCategoryMapperOrThrow(categoryId, mediaId);
        if (Boolean.TRUE.equals(requestDto.getIsPrimary()) && !Boolean.TRUE.equals(mapper.getIsPrimary())) {
            demoteCategoryPrimary(categoryId);
        }
        if (requestDto.getDisplayOrder() != null)
            mapper.setDisplayOrder(requestDto.getDisplayOrder());
        if (requestDto.getIsPrimary() != null)
            mapper.setIsPrimary(requestDto.getIsPrimary());
        if (requestDto.getAltText() != null)
            mapper.setAltText(requestDto.getAltText());
        if (requestDto.getIsActive() != null)
            mapper.setIsActive(requestDto.getIsActive());
        return toDto(categoryMediaMapperDao.update(mapper));
    }

    @Transactional
    public void detachMediaFromCategory(Long categoryId, Long mediaId) {
        categoryMediaMapperDao.delete(findCategoryMapperOrThrow(categoryId, mediaId));
    }

    @Transactional
    public void toggleCategoryAttachmentActive(Long categoryId, Long mapperId) {
        CategoryMediaMapper mapper = categoryMediaMapperDao.findById(mapperId);
        if (mapper == null)
            throw new ResourceNotFoundException("Category media mapping not found: " + mapperId);
        if (!mapper.getCategory().getId().equals(categoryId)) {
            throw new ResourceNotFoundException(
                    "Category media mapping " + mapperId + " does not belong to category " + categoryId + ".");
        }
        mapper.setIsActive(!Boolean.TRUE.equals(mapper.getIsActive()));
        categoryMediaMapperDao.update(mapper);
    }

    @Transactional(readOnly = true)
    public List<ExperienceMediaResponseDto> getCategoryMedia(Long categoryId) {
        findCategoryOrThrow(categoryId);
        return categoryMediaMapperDao.findByCategoryId(categoryId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExperienceMediaResponseDto getCategoryPrimaryMedia(Long categoryId) {
        findCategoryOrThrow(categoryId);
        CategoryMediaMapper mapper = categoryMediaMapperDao.findPrimaryByCategoryId(categoryId);
        if (mapper == null)
            throw new ResourceNotFoundException("No primary image set for category " + categoryId + ".");
        return toDto(mapper);
    }

    // ── SubCategory ───────────────────────────────────────────────────────────

    @Transactional
    public ExperienceMediaResponseDto attachMediaToSubCategory(Long subCategoryId, Long mediaId,
            ExperienceMediaAttachRequestDto requestDto) {
        SubCategory subCategory = findSubCategoryOrThrow(subCategoryId);
        Media media = findMediaOrThrow(mediaId);
        if (subCategoryMediaMapperDao.existsBySubCategoryIdAndMediaId(subCategoryId, mediaId)) {
            throw new IllegalStateException(
                    "Media " + mediaId + " is already attached to sub-category " + subCategoryId + ".");
        }
        if (Boolean.TRUE.equals(requestDto.getIsPrimary())) {
            demoteSubCategoryPrimary(subCategoryId);
        }
        boolean shouldBePrimary = Boolean.TRUE.equals(requestDto.getIsPrimary())
                || subCategoryMediaMapperDao.findPrimaryBySubCategoryId(subCategoryId) == null;

        SubCategoryMediaMapper mapper = new SubCategoryMediaMapper();
        mapper.setMedia(media);
        mapper.setDisplayOrder(requestDto.getDisplayOrder() != null ? requestDto.getDisplayOrder() : 0);
        mapper.setIsPrimary(shouldBePrimary);
        mapper.setAltText(requestDto.getAltText());
        mapper.setIsActive(requestDto.getIsActive() == null ? Boolean.TRUE : requestDto.getIsActive());
        subCategory.addMediaMapper(mapper);
        return toDto(subCategoryMediaMapperDao.save(mapper));
    }

    @Transactional
    public ExperienceMediaResponseDto updateSubCategoryAttachment(Long subCategoryId, Long mediaId,
            ExperienceMediaAttachRequestDto requestDto) {
        SubCategoryMediaMapper mapper = findSubCategoryMapperOrThrow(subCategoryId, mediaId);
        if (Boolean.TRUE.equals(requestDto.getIsPrimary()) && !Boolean.TRUE.equals(mapper.getIsPrimary())) {
            demoteSubCategoryPrimary(subCategoryId);
        }
        if (requestDto.getDisplayOrder() != null)
            mapper.setDisplayOrder(requestDto.getDisplayOrder());
        if (requestDto.getIsPrimary() != null)
            mapper.setIsPrimary(requestDto.getIsPrimary());
        if (requestDto.getAltText() != null)
            mapper.setAltText(requestDto.getAltText());
        if (requestDto.getIsActive() != null)
            mapper.setIsActive(requestDto.getIsActive());
        return toDto(subCategoryMediaMapperDao.update(mapper));
    }

    @Transactional
    public void detachMediaFromSubCategory(Long subCategoryId, Long mediaId) {
        subCategoryMediaMapperDao.delete(findSubCategoryMapperOrThrow(subCategoryId, mediaId));
    }

    @Transactional
    public void toggleSubCategoryAttachmentActive(Long subCategoryId, Long mapperId) {
        SubCategoryMediaMapper mapper = subCategoryMediaMapperDao.findById(mapperId);
        if (mapper == null)
            throw new ResourceNotFoundException("Sub-category media mapping not found: " + mapperId);
        if (!mapper.getSubCategory().getId().equals(subCategoryId)) {
            throw new ResourceNotFoundException(
                    "Sub-category media mapping " + mapperId + " does not belong to sub-category " + subCategoryId
                            + ".");
        }
        mapper.setIsActive(!Boolean.TRUE.equals(mapper.getIsActive()));
        subCategoryMediaMapperDao.update(mapper);
    }

    @Transactional(readOnly = true)
    public List<ExperienceMediaResponseDto> getSubCategoryMedia(Long subCategoryId) {
        findSubCategoryOrThrow(subCategoryId);
        return subCategoryMediaMapperDao.findBySubCategoryId(subCategoryId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExperienceMediaResponseDto getSubCategoryPrimaryMedia(Long subCategoryId) {
        findSubCategoryOrThrow(subCategoryId);
        SubCategoryMediaMapper mapper = subCategoryMediaMapperDao.findPrimaryBySubCategoryId(subCategoryId);
        if (mapper == null)
            throw new ResourceNotFoundException("No primary image set for sub-category " + subCategoryId + ".");
        return toDto(mapper);
    }

    // ── Bulk read-path enrichment (used by CategoryService / SubCategoryServiceImpl) ──

    @Transactional(readOnly = true)
    public void enrichCategoryResponses(List<CategoryResponseDto> categories) {
        if (categories == null || categories.isEmpty()) {
            return;
        }
        List<Long> categoryIds = categories.stream().map(CategoryResponseDto::getId).collect(Collectors.toList());
        Map<Long, CategoryMediaMapper> primaryByCategoryId = categoryMediaMapperDao.findPrimaryByCategoryIds(categoryIds)
                .stream()
                .collect(Collectors.toMap(
                        m -> m.getCategory().getId(),
                        m -> m,
                        (left, right) -> left,
                        LinkedHashMap::new));
        applyMedia(categories, primaryByCategoryId);

        // Full gallery: every attached image (not just primary), mirroring
        // ExperienceResponseDto.media (see ExperienceServiceImpl.getById).
        Map<Long, List<CategoryMediaMapper>> galleryByCategoryId = categoryMediaMapperDao.findByCategoryIds(categoryIds)
                .stream()
                .collect(Collectors.groupingBy(m -> m.getCategory().getId(), LinkedHashMap::new, Collectors.toList()));
        Map<Long, ImageVariantService.VariantUrls> galleryVariants = loadVariantMap(
                galleryByCategoryId.values().stream().flatMap(List::stream).collect(Collectors.toList()));
        for (CategoryResponseDto dto : categories) {
            dto.setMedia(toGalleryDtos(galleryByCategoryId.get(dto.getId()), galleryVariants));
        }

        List<SubCategoryResponseDto> allSubCategories = categories.stream()
                .filter(Objects::nonNull)
                .map(CategoryResponseDto::getSubCategories)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        enrichSubCategoryResponses(allSubCategories);
    }

    @Transactional(readOnly = true)
    public void enrichSubCategoryResponses(List<SubCategoryResponseDto> subCategories) {
        if (subCategories == null || subCategories.isEmpty()) {
            return;
        }
        List<Long> ids = subCategories.stream().map(SubCategoryResponseDto::getId).collect(Collectors.toList());
        Map<Long, SubCategoryMediaMapper> primaryBySubCategoryId = subCategoryMediaMapperDao.findPrimaryBySubCategoryIds(ids)
                .stream()
                .collect(Collectors.toMap(
                        m -> m.getSubCategory().getId(),
                        m -> m,
                        (left, right) -> left,
                        LinkedHashMap::new));
        applyMedia(subCategories, primaryBySubCategoryId);

        // Full gallery: every attached image (not just primary), mirroring
        // ExperienceResponseDto.media (see ExperienceServiceImpl.getById).
        Map<Long, List<SubCategoryMediaMapper>> galleryBySubCategoryId = subCategoryMediaMapperDao.findBySubCategoryIds(ids)
                .stream()
                .collect(Collectors.groupingBy(m -> m.getSubCategory().getId(), LinkedHashMap::new, Collectors.toList()));
        Map<Long, ImageVariantService.VariantUrls> galleryVariants = loadVariantMap(
                galleryBySubCategoryId.values().stream().flatMap(List::stream).collect(Collectors.toList()));
        for (SubCategoryResponseDto dto : subCategories) {
            dto.setMedia(toGalleryDtos(galleryBySubCategoryId.get(dto.getId()), galleryVariants));
        }
    }

    /**
     * Maps a per-owner list of junction rows to the ExperienceMediaResponseDto
     * gallery shape, applying pre-batched variant URLs (hero/thumb/original)
     * resolved once for the whole page via ImageVariantService - same bulk
     * pattern ExperienceMediaService.applyVariantUrls uses.
     */
    private List<ExperienceMediaResponseDto> toGalleryDtos(List<? extends MediaAttachmentEntity> mappers,
            Map<Long, ImageVariantService.VariantUrls> variantsByMediaId) {
        if (mappers == null || mappers.isEmpty()) {
            return new ArrayList<>();
        }
        List<ExperienceMediaResponseDto> result = new ArrayList<>();
        for (MediaAttachmentEntity mapper : mappers) {
            ExperienceMediaResponseDto dto = CatalogMediaBeanMapper.mapEntityToDto(mapper, imageUrlConfig);
            if (dto != null && dto.getMediaId() != null) {
                ImageVariantService.VariantUrls urls = variantsByMediaId.get(dto.getMediaId());
                if (urls != null) {
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
                if (dto.getOriginalUrl() == null) {
                    dto.setOriginalUrl(dto.getUrl());
                }
            }
            result.add(dto);
        }
        return result;
    }

    /**
     * Generic enrichment: works for Category and SubCategory response DTOs
     * alike via CatalogMediaCarrier, and for any MediaAttachmentEntity junction
     * row (CategoryMediaMapper / SubCategoryMediaMapper) via that interface.
     * Same hero -> original and thumb -> hero -> original fallback chain
     * ImageVariantService already applies for experience media.
     */
    private <T extends CatalogMediaCarrier> void applyMedia(List<T> items,
            Map<Long, ? extends MediaAttachmentEntity> primaryByOwnerId) {
        Map<Long, ImageVariantService.VariantUrls> variantsByMediaId = loadVariantMap(primaryByOwnerId.values());
        for (T dto : items) {
            MediaAttachmentEntity mapper = primaryByOwnerId.get(dto.getId());
            if (mapper == null || mapper.getMedia() == null) {
                continue;
            }
            Media media = mapper.getMedia();
            dto.setMediaId(media.getId());
            dto.setFileName(media.getFileName());
            dto.setStorageFileName(media.getStorageFileName());
            dto.setAltText(mapper.getAltText() != null ? mapper.getAltText() : media.getAltText());

            ImageVariantService.VariantUrls urls = variantsByMediaId.get(media.getId());
            if (urls != null) {
                dto.setHeroUrl(urls.getHeroUrl());
                dto.setThumbnailUrl(urls.getThumbnailUrl());
                dto.setOriginalUrl(urls.getOriginalUrl());
            }
            if (dto.getHeroUrl() == null) {
                dto.setHeroUrl(imageUrlConfig.buildPublicUrl(media.getStorageFileName()));
            }
            if (dto.getThumbnailUrl() == null) {
                dto.setThumbnailUrl(dto.getHeroUrl());
            }
            if (dto.getOriginalUrl() == null) {
                dto.setOriginalUrl(dto.getHeroUrl());
            }
            dto.setIcon(dto.getThumbnailUrl());
        }
    }

    private Map<Long, ImageVariantService.VariantUrls> loadVariantMap(
            Collection<? extends MediaAttachmentEntity> mappers) {
        List<Long> mediaIds = mappers.stream()
                .map(MediaAttachmentEntity::getMedia)
                .filter(Objects::nonNull)
                .map(Media::getId)
                .collect(Collectors.toList());
        return imageVariantService.getUrlsForMediaIds(mediaIds);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void demoteCategoryPrimary(Long categoryId) {
        CategoryMediaMapper current = categoryMediaMapperDao.findPrimaryByCategoryId(categoryId);
        if (current != null) {
            current.setIsPrimary(false);
            categoryMediaMapperDao.update(current);
        }
    }

    private void demoteSubCategoryPrimary(Long subCategoryId) {
        SubCategoryMediaMapper current = subCategoryMediaMapperDao.findPrimaryBySubCategoryId(subCategoryId);
        if (current != null) {
            current.setIsPrimary(false);
            subCategoryMediaMapperDao.update(current);
        }
    }

    private Category findCategoryOrThrow(Long categoryId) {
        Category category = categoryDao.findById(categoryId);
        if (category == null)
            throw new ResourceNotFoundException("Category not found: " + categoryId);
        return category;
    }

    private SubCategory findSubCategoryOrThrow(Long subCategoryId) {
        SubCategory subCategory = subCategoryDao.findById(subCategoryId);
        if (subCategory == null)
            throw new ResourceNotFoundException("SubCategory not found: " + subCategoryId);
        return subCategory;
    }

    private Media findMediaOrThrow(Long mediaId) {
        Media media = mediaDao.findById(mediaId);
        if (media == null)
            throw new ResourceNotFoundException("Media not found: " + mediaId);
        if (!Boolean.TRUE.equals(media.getIsActive()))
            throw new IllegalStateException("Media is inactive: " + mediaId);
        return media;
    }

    private CategoryMediaMapper findCategoryMapperOrThrow(Long categoryId, Long mediaId) {
        CategoryMediaMapper mapper = categoryMediaMapperDao.findByCategoryIdAndMediaId(categoryId, mediaId);
        if (mapper == null) {
            throw new ResourceNotFoundException(
                    "Media " + mediaId + " is not attached to category " + categoryId + ".");
        }
        return mapper;
    }

    private SubCategoryMediaMapper findSubCategoryMapperOrThrow(Long subCategoryId, Long mediaId) {
        SubCategoryMediaMapper mapper = subCategoryMediaMapperDao.findBySubCategoryIdAndMediaId(subCategoryId, mediaId);
        if (mapper == null) {
            throw new ResourceNotFoundException(
                    "Media " + mediaId + " is not attached to sub-category " + subCategoryId + ".");
        }
        return mapper;
    }

    // Single mapping path for both Category and SubCategory junction rows -
    // reuses CatalogMediaBeanMapper + ExperienceMediaResponseDto instead of a
    // duplicate DTO/mapper pair.
    private ExperienceMediaResponseDto toDto(MediaAttachmentEntity mapper) {
        ExperienceMediaResponseDto dto = CatalogMediaBeanMapper.mapEntityToDto(mapper, imageUrlConfig);
        enrichWithVariantUrls(dto);
        return dto;
    }

    private void enrichWithVariantUrls(ExperienceMediaResponseDto dto) {
        if (dto == null || dto.getMediaId() == null) {
            return;
        }
        ImageVariantService.VariantUrls urls = imageVariantService.getUrlsForMedia(dto.getMediaId());
        if (urls != null) {
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
        if (dto.getOriginalUrl() == null) {
            dto.setOriginalUrl(dto.getUrl());
        }
    }
}
