package com.forvmom.core.services;

import com.forvmom.common.dto.request.CancellationPolicyRequestDto;
import com.forvmom.common.dto.response.CancellationPolicyResponseDto;
import com.forvmom.common.errorhandler.ResourceNotFoundException;
import com.forvmom.core.mapper.InclusionPolicyBeanMapper;
import com.forvmom.data.dao.ExperienceCancellationPolicyDao;
import com.forvmom.data.dao.ExperienceCancellationPolicyMapperDao;
import com.forvmom.data.dao.ExperienceDao;
import com.forvmom.data.entities.Experience;
import com.forvmom.data.entities.ExperienceCancellationPolicy;
import com.forvmom.data.entities.ExperienceCancellationPolicyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * JPA-backed implementation of {@link ExperienceCancellationPolicyService}.
 *
 * <p>
 * Cancellation policies are kept as a reusable master catalog
 * ({@link ExperienceCancellationPolicy}) and linked to individual experiences
 * through {@link ExperienceCancellationPolicyMapper} junction rows that carry a
 * display order. Deleting a master policy first soft-deletes all of its junction
 * rows, so it is removed from every experience that referenced it.
 *
 * <p>
 * All operations are transactional; reads use {@code readOnly = true}. This
 * service does not touch the Redis catalog cache.
 */
@Service
public class ExperienceCancellationPolicyServiceImpl implements ExperienceCancellationPolicyService {

    @Autowired
    private ExperienceCancellationPolicyDao policyDao;

    @Autowired
    private ExperienceCancellationPolicyMapperDao policyMapperDao;

    @Autowired
    private ExperienceDao experienceDao;

    /**
     * {@inheritDoc}
     *
     * <p>
     * Creates a master policy only; it is not linked to any experience until
     * {@link #attachToExperience(Long, Long, Integer)} is called.
     *
     * @param requestDto the policy attributes
     * @return the created policy
     */
    @Override
    @Transactional
    public CancellationPolicyResponseDto createPolicy(CancellationPolicyRequestDto requestDto) {
        ExperienceCancellationPolicy entity = InclusionPolicyBeanMapper.mapRequestToPolicy(requestDto);
        ExperienceCancellationPolicy saved = policyDao.save(entity);
        return InclusionPolicyBeanMapper.mapPolicyToDto(saved);
    }

    /**
     * {@inheritDoc}
     *
     * @return all master policies, or an empty list when none exist
     */
    @Override
    @Transactional(readOnly = true)
    public List<CancellationPolicyResponseDto> getAllPolicies() {
        return policyDao.findAll().stream()
                .map(InclusionPolicyBeanMapper::mapPolicyToDto)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Updates the master row, so the change applies to every experience the policy
     * is attached to.
     *
     * @param id         the master policy identifier
     * @param requestDto the new policy attributes
     * @return the updated policy
     * @throws ResourceNotFoundException if no policy exists with the given id
     */
    @Override
    @Transactional
    public CancellationPolicyResponseDto updatePolicy(Long id, CancellationPolicyRequestDto requestDto) {
        ExperienceCancellationPolicy existing = policyDao.findById(id);
        if (existing == null)
            throw new ResourceNotFoundException("Policy not found: " + id);
        InclusionPolicyBeanMapper.updatePolicyFromRequest(existing, requestDto);
        return InclusionPolicyBeanMapper.mapPolicyToDto(policyDao.update(existing));
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * The junction rows are soft-deleted before the master row, so no experience
     * is left pointing at a deleted policy.
     *
     * @param id the master policy identifier
     * @return {@code true} once the delete has been issued
     * @throws ResourceNotFoundException if no policy exists with the given id
     */
    @Override
    @Transactional
    public boolean deletePolicy(Long id) {
        ExperienceCancellationPolicy existing = policyDao.findById(id);
        if (existing == null)
            throw new ResourceNotFoundException("Policy not found: " + id);
        // Soft-delete all junction rows first, then soft-delete master
        policyMapperDao.deleteAllByPolicyId(id);
        policyDao.delete(existing);
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * A {@code null} display order is stored as {@code 0}.
     *
     * @param experienceId the experience identifier
     * @param policyId     the master policy identifier
     * @param displayOrder the position of the policy within the experience
     * @throws IllegalStateException     if the policy is already attached to that
     *                                   experience
     * @throws ResourceNotFoundException if the experience or the policy does not
     *                                   exist
     */
    @Override
    @Transactional
    public void attachToExperience(Long experienceId, Long policyId, Integer displayOrder) {
        if (policyMapperDao.existsByExperienceIdAndPolicyId(experienceId, policyId)) {
            throw new IllegalStateException(
                    "Policy " + policyId + " is already attached to experience " + experienceId);
        }
        Experience experience = experienceDao.findById(experienceId);
        if (experience == null)
            throw new ResourceNotFoundException("Experience not found: " + experienceId);

        ExperienceCancellationPolicy policy = policyDao.findById(policyId);
        if (policy == null)
            throw new ResourceNotFoundException("Policy not found: " + policyId);

        ExperienceCancellationPolicyMapper mapper = new ExperienceCancellationPolicyMapper();
        mapper.setPolicy(policy);
        mapper.setExperience(experience);
        mapper.setDisplayOrder(displayOrder != null ? displayOrder : 0);
        experience.addPolicyMapper(mapper);
        policyMapperDao.save(mapper);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Only the junction row is removed; the master policy stays available for
     * other experiences.
     *
     * @param experienceId the experience identifier
     * @param policyId     the master policy identifier
     * @throws ResourceNotFoundException if the policy is not attached to that
     *                                   experience
     */
    @Override
    @Transactional
    public void detachFromExperience(Long experienceId, Long policyId) {
        ExperienceCancellationPolicyMapper mapper = policyMapperDao.findByExperienceIdAndPolicyId(experienceId,
                policyId);
        if (mapper == null) {
            throw new ResourceNotFoundException(
                    "Policy " + policyId + " is not attached to experience " + experienceId);
        }
        policyMapperDao.delete(mapper);
    }

    /**
     * {@inheritDoc}
     *
     * @param experienceId the experience identifier
     * @return the policies attached to that experience, or an empty list
     */
    @Override
    @Transactional(readOnly = true)
    public List<CancellationPolicyResponseDto> getPoliciesForExperience(Long experienceId) {
        List<ExperienceCancellationPolicyMapper> mappers = policyMapperDao.findByExperienceId(experienceId);
        return InclusionPolicyBeanMapper.mapPolicyMappers(mappers);
    }
}
