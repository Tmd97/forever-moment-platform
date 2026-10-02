package com.forvmom.data.dao;

import com.forvmom.data.entities.ExperienceLocationPincodeMapper;

import java.util.List;

public interface ExperienceLocationPincodeMapperDao extends GenericDao<ExperienceLocationPincodeMapper, Long> {

    /** Pincode restrictions on an experience-location mapping; empty = unrestricted */
    List<ExperienceLocationPincodeMapper> findByMapperId(Long mapperId);

    ExperienceLocationPincodeMapper findByMapperIdAndPincodeId(Long mapperId, Long pincodeId);

    boolean existsByMapperIdAndPincodeId(Long mapperId, Long pincodeId);

    /** Soft-delete all pincode restrictions for a mapper (used by "replace") */
    void deleteAllByMapperId(Long mapperId);

    /**
     * Bulk serviceability query for catalog listing: returns the ids of
     * experiences attached (and active) to {@code locationId} whose
     * attachment is either unrestricted (no pincode rows) or explicitly
     * includes {@code pincodeId}.
     *
     * @param locationId the location the pincode belongs to
     * @param pincodeId  the pincode being serviced
     * @return the serviceable experience ids
     */
    List<Long> findServiceableExperienceIds(Long locationId, Long pincodeId);
}
