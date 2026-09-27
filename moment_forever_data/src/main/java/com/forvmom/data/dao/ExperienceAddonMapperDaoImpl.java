package com.forvmom.data.dao;

import com.forvmom.data.entities.ExperienceAddonMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public class ExperienceAddonMapperDaoImpl extends GenericDaoImpl<ExperienceAddonMapper, Long>
        implements ExperienceAddonMapperDao {

    public ExperienceAddonMapperDaoImpl() {
        super(ExperienceAddonMapper.class);
    }

    @Override
    public List<ExperienceAddonMapper> findByExperienceId(Long experienceId) {
        return em.createQuery(
                "SELECT m FROM ExperienceAddonMapper m " +
                        "JOIN FETCH m.addon a " +
                        "LEFT JOIN FETCH a.imageMedia im " +
                        "WHERE m.experience.id = :expId AND m.deleted = false " +
                        "ORDER BY a.name ASC",
                ExperienceAddonMapper.class)
                .setParameter("expId", experienceId)
                .getResultList();
    }

    @Override
    public boolean existsByExperienceIdAndAddonId(Long experienceId, Long addonId) {
        Long count = em.createQuery(
                "SELECT COUNT(m) FROM ExperienceAddonMapper m " +
                        "WHERE m.experience.id = :expId AND m.addon.id = :addonId AND m.deleted = false",
                Long.class)
                .setParameter("expId", experienceId)
                .setParameter("addonId", addonId)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public ExperienceAddonMapper findByExperienceIdAndAddonId(Long experienceId, Long addonId) {
        List<ExperienceAddonMapper> results = em.createQuery(
                "SELECT m FROM ExperienceAddonMapper m " +
                        "WHERE m.experience.id = :expId AND m.addon.id = :addonId AND m.deleted = false",
                ExperienceAddonMapper.class)
                .setParameter("expId", experienceId)
                .setParameter("addonId", addonId)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public void deleteAllByExperienceId(Long experienceId) {
        em.createQuery(
                "UPDATE ExperienceAddonMapper m SET m.deleted = true " +
                        "WHERE m.experience.id = :expId AND m.deleted = false")
                .setParameter("expId", experienceId)
                .executeUpdate();
    }

    @Override
    public void deleteAllByAddonId(Long addonId) {
        em.createQuery(
                "UPDATE ExperienceAddonMapper m SET m.deleted = true " +
                        "WHERE m.addon.id = :addonId AND m.deleted = false")
                .setParameter("addonId", addonId)
                .executeUpdate();
    }
}
