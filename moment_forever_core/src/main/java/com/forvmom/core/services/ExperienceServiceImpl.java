package com.forvmom.core.services;

import com.forvmom.common.dto.request.ExperienceCreateRequestDto;
import com.forvmom.common.dto.request.ExperienceDetailRequestDto;
import com.forvmom.common.dto.response.ExperienceDetailResponseDto;
import com.forvmom.common.dto.response.ExperienceHighlightResponseDto;
import com.forvmom.common.dto.response.ExperienceResponseDto;
import com.forvmom.common.errorhandler.ResourceNotFoundException;
import com.forvmom.core.config.ImageUrlConfig;
import com.forvmom.core.mapper.ExperienceBeanMapper;
import com.forvmom.core.mapper.ExperienceMediaBeanMapper;
import com.forvmom.core.mapper.InclusionPolicyBeanMapper;
import com.forvmom.data.dao.ExperienceDao;
import com.forvmom.data.dao.ExperienceDetailDao;
import com.forvmom.data.dao.ExperienceMediaMapperDao;
import com.forvmom.data.dao.SubCategoryDao;
import com.forvmom.data.entities.Experience;
import com.forvmom.data.entities.ExperienceDetail;
import com.forvmom.data.entities.SubCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JPA-backed implementation of {@link ExperienceService}, the service that owns
 * the {@link Experience} aggregate and its one-to-one
 * {@link ExperienceDetail} row.
 *
 * <p>
 * An experience is identified publicly by a unique slug and always belongs to a
 * {@link SubCategory}. Creation persists both the experience row and its detail
 * row; updates upsert the detail row.
 *
 * <p>
 * Reads deliberately issue several queries instead of one wide join: the first
 * fetch-joins detail, sub-category and inclusion mappers, and follow-up queries
 * load policy mappers, location/time-slot mappers and media mappers into the
 * same Hibernate session. Splitting them avoids the Cartesian product that a
 * single multi-collection join would produce.
 *
 * <p>
 * Write paths keep the Redis catalog snapshot in step by calling
 * {@code CatalogCacheService} inline: {@code updateExperience},
 * {@code toggleActive} and {@code toggleFeatured} warm the snapshot, while
 * {@code deleteExperience} evicts it, so the asynchronous booking enrichment
 * path never reads stale catalog data.
 */
@Service
public class ExperienceServiceImpl implements ExperienceService {

    @Autowired
    private ExperienceDao experienceDao;

    @Autowired
    private ExperienceDetailDao experienceDetailDao;

    @Autowired
    private SubCategoryDao subCategoryDao;

    @Autowired
    private CatalogCacheService catalogCacheService;

    @Autowired
    private ExperienceMediaMapperDao experienceMediaMapperDao;

    @Autowired
    private ImageUrlConfig imageUrlConfig;

    @Autowired
    private ImageFlowCacheService imageFlowCacheService;

    @Autowired
    private ExperienceMediaService experienceMediaService;

    /**
     * {@inheritDoc}
     *
     * <p>
     * Persists the experience row and its detail row in the same transaction. Note
     * that no cache warming happens here, since a brand new experience has no
     * location or slot mappings to snapshot yet.
     *
     * @param requestDto the experience attributes, including detail fields and the
     *                   owning sub-category id
     * @return the created experience
     * @throws IllegalArgumentException  if the slug is already in use
     * @throws ResourceNotFoundException if the sub-category does not exist
     */
    @Override
    @Transactional
    public ExperienceResponseDto createExperience(ExperienceCreateRequestDto requestDto) {
        if (experienceDao.existsBySlug(requestDto.getSlug())) {
            throw new IllegalArgumentException("Experience with slug '" + requestDto.getSlug() + "' already exists");
        }

        SubCategory subCategory = subCategoryDao.findById(requestDto.getSubCategoryId());
        if (subCategory == null) {
            throw new ResourceNotFoundException("SubCategory not found with id " + requestDto.getSubCategoryId());
        }

        // Save basic experience row
        Experience experience = new Experience();
        ExperienceBeanMapper.mapCreateDtoToEntity(requestDto, experience);
        experience.setSubCategory(subCategory);
        Experience saved = experienceDao.save(experience);

        // Save detail row (always created with the experience)
        ExperienceDetail detail = new ExperienceDetail();
        ExperienceBeanMapper.mapCreateDtoToDetail(requestDto, detail);
        detail.setExperience(saved);
        ExperienceDetail savedDetail = experienceDetailDao.save(detail);
        saved.setDetail(savedDetail);

        return ExperienceBeanMapper.mapEntityToDto(saved, true);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The slug uniqueness check runs only when the slug changes, and the
     * sub-category is re-resolved only when a different id is supplied. The detail
     * row is upserted: inserted when missing, updated otherwise. Finally the Redis
     * catalog snapshot is warmed inline so booking enrichment sees the new values.
     *
     * @param id         the experience identifier
     * @param requestDto the new experience and detail attributes
     * @return the updated experience
     * @throws ResourceNotFoundException if the experience or the new sub-category
     *                                   does not exist
     * @throws IllegalArgumentException  if the new slug is already in use
     */
    @Override
    @Transactional
    public ExperienceResponseDto updateExperience(Long id, ExperienceCreateRequestDto requestDto) {
        Experience existing = experienceDao.findByIdWithDetail(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Experience not found with id " + id);
        }

        if (!existing.getSlug().equals(requestDto.getSlug()) &&
                experienceDao.existsBySlug(requestDto.getSlug())) {
            throw new IllegalArgumentException("Experience with slug '" + requestDto.getSlug() + "' already exists");
        }

        if (!existing.getSubCategory().getId().equals(requestDto.getSubCategoryId())) {
            SubCategory subCategory = subCategoryDao.findById(requestDto.getSubCategoryId());
            if (subCategory == null) {
                throw new ResourceNotFoundException("SubCategory not found with id " + requestDto.getSubCategoryId());
            }
            existing.setSubCategory(subCategory);
        }

        // Update basic fields
        ExperienceBeanMapper.mapCreateDtoToEntity(requestDto, existing);
        Experience updated = experienceDao.update(existing);

        // Upsert detail
        ExperienceDetail detail = updated.getDetail();
        if (detail == null) {
            detail = new ExperienceDetail();
            detail.setExperience(updated);
        }
        ExperienceBeanMapper.mapCreateDtoToDetail(requestDto, detail);
        ExperienceDetail savedDetail = (detail.getId() == null)
                ? experienceDetailDao.save(detail)
                : experienceDetailDao.update(detail);
        updated.setDetail(savedDetail);

        catalogCacheService.warmExperienceCache(updated);
        imageFlowCacheService.evictExperienceDetail(id);

        return ExperienceBeanMapper.mapEntityToDto(updated, true);
    }

    /*
     * Your current approach with 2 queries is actually quite good:
     * Query 1: Fetches Experience + Detail + SubCategory + InclusionMappers (with
     * JOIN FETCH)
     * Query 2: Fetches PolicyMappers separately (avoids Cartesian product with
     * inclusions)
     * This avoids the multiplied result set problem that would occur if you joined
     * both collections in one query.
     */
    /**
     * {@inheritDoc}
     *
     * <p>
     * Loads the full experience view with four queries (detail plus inclusions,
     * then policies, then locations/time slots, then media gallery), letting
     * Hibernate merge the extra collections into the already-managed entity.
     *
     * @param id the experience identifier
     * @return the experience with inclusions, cancellation policies and locations
     * @throws ResourceNotFoundException if no experience exists with the given id
     */
    @Override
    @Transactional(readOnly = true)
    public ExperienceResponseDto getById(Long id) {
        ExperienceResponseDto cached = imageFlowCacheService.getExperienceDetail(id);
        if (cached != null) {
            return cached;
        }

        // Query 1: experience + detail + subCategory + inclusionMappers (JOIN FETCH)
        Experience experience = experienceDao.findByIdWithDetail(id);
        if (experience == null) {
            throw new ResourceNotFoundException("Experience not found with id " + id);
        }
        // Query 2: policyMappers (Hibernate merges into same session entity)
        experienceDao.findByIdWithPolicies(id);
        // Query 3: locationMappers + location + timeslotMappers + timeSlot
        experienceDao.findByIdWithLocations(id);

        ExperienceResponseDto dto = ExperienceBeanMapper.mapEntityToDto(experience, true);
        dto.setInclusions(InclusionPolicyBeanMapper.mapInclusionMappers(
                new ArrayList<>(experience.getInclusionMappers())));
        dto.setCancellationPolicies(InclusionPolicyBeanMapper.mapPolicyMappers(
                new ArrayList<>(experience.getPolicyMappers())));
        dto.setLocations(ExperienceBeanMapper.mapLocationMappers(
                new ArrayList<>(experience.getLocationMappers())));
        dto.setMedia(ExperienceMediaBeanMapper.mapEntitiesToDto(
                experienceMediaMapperDao.findByExperienceId(id), imageUrlConfig));
        experienceMediaService.applyVariantUrls(dto.getMedia());
        imageFlowCacheService.putExperienceDetail(id, dto);
        return dto;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Slug-based counterpart of {@link #getById(Long)}, using the same multi-query
     * loading strategy.
     *
     * @param slug the public experience slug
     * @return the experience with inclusions, cancellation policies and locations
     * @throws ResourceNotFoundException if no experience has that slug
     */
    @Override
    @Transactional(readOnly = true)
    public ExperienceResponseDto getBySlug(String slug) {
        Experience experience = experienceDao.findBySlug(slug);
        if (experience == null) {
            throw new ResourceNotFoundException("Experience not found with slug '" + slug + "'");
        }
        return getById(experience.getId());
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Returns the lightweight highlight projection rather than the full detail
     * view.
     *
     * @return all experiences as highlights, or an empty list when none exist
     */
    @Override
    @Transactional(readOnly = true)
    public List<ExperienceHighlightResponseDto> getAll() {
        List<Experience> experiences = experienceDao.findAllWithDetail();
        if (experiences == null || experiences.isEmpty())
            return new ArrayList<>();
        return experiences.stream()
                .map(ExperienceBeanMapper::mapEntityToHighlightDto)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * @return the active experiences as highlights, or an empty list
     */
    @Override
    @Transactional(readOnly = true)
    public List<ExperienceHighlightResponseDto> getAllActive() {
        List<Experience> experiences = experienceDao.findAllActive();
        if (experiences == null || experiences.isEmpty())
            return new ArrayList<>();
        return experiences.stream()
                .map(ExperienceBeanMapper::mapEntityToHighlightDto)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Unlike the other list methods, an empty result is reported as an error.
     *
     * @param subCategoryId the sub-category identifier
     * @return the experiences of that sub-category as highlights
     * @throws ResourceNotFoundException if the sub-category has no experiences
     */
    @Override
    @Transactional(readOnly = true)
    public List<ExperienceHighlightResponseDto> getBySubCategory(Long subCategoryId) {
        List<Experience> experiences = experienceDao.findBySubCategoryId(subCategoryId);
        if (experiences == null || experiences.isEmpty()) {
            throw new ResourceNotFoundException("No experiences found for sub-category id " + subCategoryId);
        }
        return experiences.stream()
                .map(ExperienceBeanMapper::mapEntityToHighlightDto)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * @return the featured experiences as highlights, or an empty list
     */
    @Override
    @Transactional(readOnly = true)
    public List<ExperienceHighlightResponseDto> getFeatured() {
        List<Experience> experiences = experienceDao.findFeatured();
        if (experiences == null || experiences.isEmpty())
            return new ArrayList<>();
        return experiences.stream()
                .map(ExperienceBeanMapper::mapEntityToHighlightDto)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Soft-deletes the experience (the entity carries {@code @SQLDelete}) and then
     * evicts its Redis catalog snapshot so the removed experience cannot be served
     * from cache.
     *
     * @param id the experience identifier
     * @return {@code true} once the delete has been issued
     * @throws ResourceNotFoundException if no experience exists with the given id
     */
    @Override
    @Transactional
    public boolean deleteExperience(Long id) {
        Experience existing = experienceDao.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Experience not found with id " + id);
        }
        // Soft-delete the experience — @SQLDelete on Experience fires UPDATE; the
        // @OneToMany(cascade=ALL, orphanRemoval=true) on inclusionMappers/policyMappers
        // means Hibernate will also remove the junction rows within the same
        // transaction.
        experienceDao.delete(existing);

        // Evict from cache
        catalogCacheService.evictExperience(id);
        imageFlowCacheService.evictExperienceDetail(id);

        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Flips the active flag and re-warms the catalog snapshot with the new state.
     *
     * @param id the experience identifier
     * @throws ResourceNotFoundException if no experience exists with the given id
     */
    @Override
    @Transactional
    public void toggleActive(Long id) {
        Experience existing = experienceDao.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Experience not found with id " + id);
        }
        existing.setActive(!existing.isActive());
        Experience updated = experienceDao.update(existing);
        catalogCacheService.warmExperienceCache(updated);
        imageFlowCacheService.evictExperienceDetail(id);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Flips the featured flag and re-warms the catalog snapshot with the new
     * state.
     *
     * @param id the experience identifier
     * @throws ResourceNotFoundException if no experience exists with the given id
     */
    @Override
    @Transactional
    public void toggleFeatured(Long id) {
        Experience existing = experienceDao.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Experience not found with id " + id);
        }
        existing.setIsFeatured(!existing.getIsFeatured());
        Experience updated = experienceDao.update(existing);
        catalogCacheService.warmExperienceCache(updated);
        imageFlowCacheService.evictExperienceDetail(id);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Inserts the detail row when the experience has none yet, otherwise updates
     * the existing one.
     *
     * @param experienceId the experience identifier
     * @param requestDto   the detail fields to store
     * @return the stored detail
     * @throws ResourceNotFoundException if no experience exists with the given id
     */
    @Override
    @Transactional
    public ExperienceDetailResponseDto upsertDetail(Long experienceId, ExperienceDetailRequestDto requestDto) {
        Experience experience = experienceDao.findById(experienceId);
        if (experience == null) {
            throw new ResourceNotFoundException("Experience not found with id " + experienceId);
        }

        ExperienceDetail detail = experienceDetailDao.findByExperienceId(experienceId);
        if (detail == null) {
            detail = new ExperienceDetail();
            detail.setExperience(experience);
        }

        ExperienceBeanMapper.mapDetailDtoToEntity(requestDto, detail);
        ExperienceDetail saved = (detail.getId() == null)
                ? experienceDetailDao.save(detail)
                : experienceDetailDao.update(detail);
        imageFlowCacheService.evictExperienceDetail(experienceId);

        return ExperienceBeanMapper.mapDetailToDto(saved);
    }

    /**
     * {@inheritDoc}
     *
     * @param experienceId the experience identifier
     * @return the detail row of that experience
     * @throws ResourceNotFoundException if the experience has no detail row
     */
    @Override
    @Transactional(readOnly = true)
    public ExperienceDetailResponseDto getDetail(Long experienceId) {
        ExperienceDetail detail = experienceDetailDao.findByExperienceId(experienceId);
        if (detail == null) {
            throw new ResourceNotFoundException("No detail found for experience id " + experienceId);
        }
        return ExperienceBeanMapper.mapDetailToDto(detail);
    }
}
