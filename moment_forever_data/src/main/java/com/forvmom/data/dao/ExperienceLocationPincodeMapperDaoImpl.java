package com.forvmom.data.dao;

import com.forvmom.data.entities.ExperienceLocationPincodeMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public class ExperienceLocationPincodeMapperDaoImpl extends GenericDaoImpl<ExperienceLocationPincodeMapper, Long>
        implements ExperienceLocationPincodeMapperDao {

    public ExperienceLocationPincodeMapperDaoImpl() {
        super(ExperienceLocationPincodeMapper.class);
    }

    @Override
    public List<ExperienceLocationPincodeMapper> findByMapperId(Long mapperId) {
        return em.createQuery(
                "SELECT p FROM ExperienceLocationPincodeMapper p " +
                        "JOIN FETCH p.pincode " +
                        "WHERE p.experienceLocationMapper.id = :mapperId AND p.deleted = false",
                ExperienceLocationPincodeMapper.class)
                .setParameter("mapperId", mapperId)
                .getResultList();
    }

    @Override
    public ExperienceLocationPincodeMapper findByMapperIdAndPincodeId(Long mapperId, Long pincodeId) {
        List<ExperienceLocationPincodeMapper> results = em.createQuery(
                "SELECT p FROM ExperienceLocationPincodeMapper p " +
                        "WHERE p.experienceLocationMapper.id = :mapperId AND p.pincode.id = :pincodeId " +
                        "AND p.deleted = false",
                ExperienceLocationPincodeMapper.class)
                .setParameter("mapperId", mapperId)
                .setParameter("pincodeId", pincodeId)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public boolean existsByMapperIdAndPincodeId(Long mapperId, Long pincodeId) {
        Long count = em.createQuery(
                "SELECT COUNT(p) FROM ExperienceLocationPincodeMapper p " +
                        "WHERE p.experienceLocationMapper.id = :mapperId AND p.pincode.id = :pincodeId " +
                        "AND p.deleted = false",
                Long.class)
                .setParameter("mapperId", mapperId)
                .setParameter("pincodeId", pincodeId)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public void deleteAllByMapperId(Long mapperId) {
        em.createQuery(
                "UPDATE ExperienceLocationPincodeMapper p SET p.deleted = true " +
                        "WHERE p.experienceLocationMapper.id = :mapperId AND p.deleted = false")
                .setParameter("mapperId", mapperId)
                .executeUpdate();
    }

    @Override
    public List<Long> findServiceableExperienceIds(Long locationId, Long pincodeId) {
        return em.createQuery(
                "SELECT DISTINCT m.experience.id FROM ExperienceLocationMapper m " +
                        "WHERE m.location.id = :locationId AND m.isActive = true AND m.deleted = false " +
                        "AND ( " +
                        "  NOT EXISTS (SELECT 1 FROM ExperienceLocationPincodeMapper p " +
                        "              WHERE p.experienceLocationMapper = m AND p.deleted = false) " +
                        "  OR EXISTS (SELECT 1 FROM ExperienceLocationPincodeMapper p " +
                        "             WHERE p.experienceLocationMapper = m AND p.pincode.id = :pincodeId " +
                        "             AND p.isActive = true AND p.deleted = false) " +
                        ")",
                Long.class)
                .setParameter("locationId", locationId)
                .setParameter("pincodeId", pincodeId)
                .getResultList();
    }
}
