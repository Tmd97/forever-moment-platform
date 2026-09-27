package com.forvmom.core.services;

import com.forvmom.common.dto.request.ExperienceInclusionRequestDto;
import com.forvmom.common.dto.response.ExperienceInclusionResponseDto;
import com.forvmom.common.errorhandler.ResourceNotFoundException;
import com.forvmom.core.mapper.InclusionPolicyBeanMapper;
import com.forvmom.data.dao.ExperienceDao;
import com.forvmom.data.dao.ExperienceInclusionDao;
import com.forvmom.data.dao.ExperienceInclusionMapperDao;
import com.forvmom.data.entities.Experience;
import com.forvmom.data.entities.ExperienceInclusion;
import com.forvmom.data.entities.ExperienceInclusionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * JPA-backed implementation of {@link ExperienceInclusionService}.
 *
 * <p>
 * Inclusions are maintained as a reusable master catalog
 * ({@link ExperienceInclusion}) that is attached to individual experiences
 * through {@link ExperienceInclusionMapper} junction rows carrying a display
 * order. Deleting a master inclusion first soft-deletes all of its junction
 * rows, so it disappears from every experience it was attached to.
 *
 * <p>
 * All operations are transactional; reads use {@code readOnly = true}. This
 * service does not touch the Redis catalog cache.
 */
@Service
public class ExperienceInclusionServiceImpl implements ExperienceInclusionService {

    @Autowired
    private ExperienceInclusionDao inclusionDao;

    @Autowired
    private ExperienceInclusionMapperDao inclusionMapperDao;

    @Autowired
    private ExperienceDao experienceDao;

    /**
     * {@inheritDoc}
     *
     * <p>
     * Creates a master inclusion only; it is not linked to any experience until
     * {@link #attachToExperience(Long, Long, Integer)} is called.
     *
     * @param requestDto the inclusion attributes
     * @return the created inclusion
     */
    @Override
    @Transactional
    public ExperienceInclusionResponseDto createInclusion(ExperienceInclusionRequestDto requestDto) {
        ExperienceInclusion entity = InclusionPolicyBeanMapper.mapRequestToInclusion(requestDto);
        ExperienceInclusion saved = inclusionDao.save(entity);
        return InclusionPolicyBeanMapper.mapInclusionToDto(saved);
    }

    /**
     * {@inheritDoc}
     *
     * @return all master inclusions, or an empty list when none exist
     */
    @Override
    @Transactional(readOnly = true)
    public List<ExperienceInclusionResponseDto> getAllInclusions() {
        return inclusionDao.findAll().stream()
                .map(InclusionPolicyBeanMapper::mapInclusionToDto)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Updates the master row, so the change is visible to every experience the
     * inclusion is attached to.
     *
     * @param id         the master inclusion identifier
     * @param requestDto the new inclusion attributes
     * @return the updated inclusion
     * @throws ResourceNotFoundException if no inclusion exists with the given id
     */
    @Override
    @Transactional
    public ExperienceInclusionResponseDto updateInclusion(Long id, ExperienceInclusionRequestDto requestDto) {
        ExperienceInclusion existing = inclusionDao.findById(id);
        if (existing == null)
            throw new ResourceNotFoundException("Inclusion not found: " + id);
        InclusionPolicyBeanMapper.updateInclusionFromRequest(existing, requestDto);
        return InclusionPolicyBeanMapper.mapInclusionToDto(inclusionDao.update(existing));
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The junction rows are soft-deleted before the master row, so no experience
     * is left pointing at a deleted inclusion.
     *
     * @param id the master inclusion identifier
     * @return {@code true} once the delete has been issued
     * @throws ResourceNotFoundException if no inclusion exists with the given id
     */
    @Override
    @Transactional
    public boolean deleteInclusion(Long id) {
        ExperienceInclusion existing = inclusionDao.findById(id);
        if (existing == null)
            throw new ResourceNotFoundException("Inclusion not found: " + id);
        // Soft-delete all junction rows (removes it from all experiences) then
        // soft-delete master
        inclusionMapperDao.deleteAllByInclusionId(id);
        inclusionDao.delete(existing);
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * A {@code null} display order is stored as {@code 0}.
     *
     * @param experienceId the experience identifier
     * @param inclusionId  the master inclusion identifier
     * @param displayOrder the position of the inclusion within the experience
     * @throws IllegalStateException     if the inclusion is already attached to
     *                                   that experience
     * @throws ResourceNotFoundException if the experience or the inclusion does not
     *                                   exist
     */
    @Override
    @Transactional
    public void attachToExperience(Long experienceId, Long inclusionId, Integer displayOrder) {
        if (inclusionMapperDao.existsByExperienceIdAndInclusionId(experienceId, inclusionId)) {
            throw new IllegalStateException(
                    "Inclusion " + inclusionId + " is already attached to experience " + experienceId);
        }
        Experience experience = experienceDao.findById(experienceId);
        if (experience == null)
            throw new ResourceNotFoundException("Experience not found: " + experienceId);

        ExperienceInclusion inclusion = inclusionDao.findById(inclusionId);
        if (inclusion == null)
            throw new ResourceNotFoundException("Inclusion not found: " + inclusionId);

        ExperienceInclusionMapper mapper = new ExperienceInclusionMapper();
        mapper.setInclusion(inclusion);
        mapper.setDisplayOrder(displayOrder != null ? displayOrder : 0);
        // Helper wires both sides: mapper.setExperience(experience) +
        // experience.getInclusionMappers().add(mapper)
        experience.addInclusionMapper(mapper);
        inclusionMapperDao.save(mapper);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Only the junction row is removed; the master inclusion remains available for
     * other experiences.
     *
     * @param experienceId the experience identifier
     * @param inclusionId  the master inclusion identifier
     * @throws ResourceNotFoundException if the inclusion is not attached to that
     *                                   experience
     */
    @Override
    @Transactional
    public void detachFromExperience(Long experienceId, Long inclusionId) {
        ExperienceInclusionMapper mapper = inclusionMapperDao.findByExperienceIdAndInclusionId(experienceId,
                inclusionId);
        if (mapper == null) {
            throw new ResourceNotFoundException(
                    "Inclusion " + inclusionId + " is not attached to experience " + experienceId);
        }
        inclusionMapperDao.delete(mapper);
    }

    /**
     * {@inheritDoc}
     *
     * @param experienceId the experience identifier
     * @return the inclusions attached to that experience, or an empty list
     */
    @Override
    @Transactional(readOnly = true)
    public List<ExperienceInclusionResponseDto> getInclusionsForExperience(Long experienceId) {
        List<ExperienceInclusionMapper> mappers = inclusionMapperDao.findByExperienceId(experienceId);
        return InclusionPolicyBeanMapper.mapInclusionMappers(mappers);
    }
}
