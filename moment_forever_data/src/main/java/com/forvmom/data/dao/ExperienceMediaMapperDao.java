package com.forvmom.data.dao;

import com.forvmom.data.entities.ExperienceMediaMapper;

import java.util.List;

public interface ExperienceMediaMapperDao extends GenericDao<ExperienceMediaMapper, Long> {

    /** All active media mappers for a given experience, ordered by displayOrder */
    List<ExperienceMediaMapper> findByExperienceId(Long experienceId);

    /** Check if a specific media is already attached to an experience */
    boolean existsByExperienceIdAndMediaId(Long experienceId, Long mediaId);

    /** Look up a specific junction row */
    ExperienceMediaMapper findByExperienceIdAndMediaId(Long experienceId, Long mediaId);

    /** Find the current primary/cover image mapper for an experience */
    ExperienceMediaMapper findPrimaryByExperienceId(Long experienceId);

    /** Find all experiences that currently reference a media record */
    List<Long> findExperienceIdsByMediaId(Long mediaId);
}
