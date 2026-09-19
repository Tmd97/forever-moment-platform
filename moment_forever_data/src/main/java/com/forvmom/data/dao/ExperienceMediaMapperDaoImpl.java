package com.forvmom.data.dao;

import com.forvmom.data.entities.ExperienceMediaMapper;
import jakarta.persistence.NoResultException;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ExperienceMediaMapperDaoImpl extends GenericDaoImpl<ExperienceMediaMapper, Long>
        implements ExperienceMediaMapperDao {

    public ExperienceMediaMapperDaoImpl() {
        super(ExperienceMediaMapper.class);
    }

    @Override
    public List<ExperienceMediaMapper> findByExperienceId(Long experienceId) {
        return em.createQuery(
                "SELECT m FROM ExperienceMediaMapper m " +
                        "JOIN FETCH m.media med " +
                        "WHERE m.experience.id = :expId AND m.deleted = false " +
                        "ORDER BY m.displayOrder ASC, m.createdOn ASC",
                ExperienceMediaMapper.class)
                .setParameter("expId", experienceId)
                .getResultList();
    }

    @Override
    public boolean existsByExperienceIdAndMediaId(Long experienceId, Long mediaId) {
        Long count = em.createQuery(
                "SELECT COUNT(m) FROM ExperienceMediaMapper m " +
                        "WHERE m.experience.id = :expId AND m.media.id = :mediaId AND m.deleted = false",
                Long.class)
                .setParameter("expId", experienceId)
                .setParameter("mediaId", mediaId)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public ExperienceMediaMapper findByExperienceIdAndMediaId(Long experienceId, Long mediaId) {
        try {
            return em.createQuery(
                    "SELECT m FROM ExperienceMediaMapper m " +
                            "JOIN FETCH m.media med " +
                            "WHERE m.experience.id = :expId AND m.media.id = :mediaId AND m.deleted = false",
                    ExperienceMediaMapper.class)
                    .setParameter("expId", experienceId)
                    .setParameter("mediaId", mediaId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public ExperienceMediaMapper findPrimaryByExperienceId(Long experienceId) {
        try {
            return em.createQuery(
                    "SELECT m FROM ExperienceMediaMapper m " +
                            "JOIN FETCH m.media med " +
                            "WHERE m.experience.id = :expId AND m.isPrimary = true AND m.deleted = false",
                    ExperienceMediaMapper.class)
                    .setParameter("expId", experienceId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<Long> findExperienceIdsByMediaId(Long mediaId) {
        return em.createQuery(
                "SELECT DISTINCT m.experience.id FROM ExperienceMediaMapper m " +
                        "WHERE m.media.id = :mediaId AND m.deleted = false",
                Long.class)
                .setParameter("mediaId", mediaId)
                .getResultList();
    }
}
