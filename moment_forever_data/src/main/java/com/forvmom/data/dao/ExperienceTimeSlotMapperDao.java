package com.forvmom.data.dao;

import com.forvmom.data.entities.ExperienceTimeSlotMapper;

import java.util.List;

public interface ExperienceTimeSlotMapperDao extends GenericDao<ExperienceTimeSlotMapper, Long> {

    /** All timeslot mappers for a specific experience-location pair */
    List<ExperienceTimeSlotMapper> findByExperienceLocationId(Long expLocationId);

    boolean existsByExperienceLocationIdAndTimeSlotId(Long expLocationId, Long timeSlotId);

    ExperienceTimeSlotMapper findByExperienceLocationIdAndTimeSlotId(Long expLocationId, Long timeSlotId);

    /**
     * Soft-delete all timeslot mappers for an experience-location (on location
     * detach or experience delete)
     */
    void deleteAllByExperienceLocationId(Long expLocationId);

}
