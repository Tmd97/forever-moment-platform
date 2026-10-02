package com.forvmom.data.dao;

import com.forvmom.data.entities.SubCategoryMediaMapper;
import jakarta.persistence.NoResultException;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class SubCategoryMediaMapperDaoImpl extends GenericDaoImpl<SubCategoryMediaMapper, Long>
        implements SubCategoryMediaMapperDao {

    public SubCategoryMediaMapperDaoImpl() {
        super(SubCategoryMediaMapper.class);
    }

    @Override
    public List<SubCategoryMediaMapper> findBySubCategoryId(Long subCategoryId) {
        return em.createQuery(
                "SELECT m FROM SubCategoryMediaMapper m " +
                        "JOIN FETCH m.media med " +
                        "WHERE m.subCategory.id = :subCategoryId AND m.deleted = false " +
                        "ORDER BY m.displayOrder ASC, m.createdOn ASC",
                SubCategoryMediaMapper.class)
                .setParameter("subCategoryId", subCategoryId)
                .getResultList();
    }

    @Override
    public boolean existsBySubCategoryIdAndMediaId(Long subCategoryId, Long mediaId) {
        Long count = em.createQuery(
                "SELECT COUNT(m) FROM SubCategoryMediaMapper m " +
                        "WHERE m.subCategory.id = :subCategoryId AND m.media.id = :mediaId AND m.deleted = false",
                Long.class)
                .setParameter("subCategoryId", subCategoryId)
                .setParameter("mediaId", mediaId)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public SubCategoryMediaMapper findBySubCategoryIdAndMediaId(Long subCategoryId, Long mediaId) {
        try {
            return em.createQuery(
                    "SELECT m FROM SubCategoryMediaMapper m " +
                            "JOIN FETCH m.media med " +
                            "WHERE m.subCategory.id = :subCategoryId AND m.media.id = :mediaId AND m.deleted = false",
                    SubCategoryMediaMapper.class)
                    .setParameter("subCategoryId", subCategoryId)
                    .setParameter("mediaId", mediaId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public SubCategoryMediaMapper findPrimaryBySubCategoryId(Long subCategoryId) {
        try {
            return em.createQuery(
                    "SELECT m FROM SubCategoryMediaMapper m " +
                            "JOIN FETCH m.media med " +
                            "WHERE m.subCategory.id = :subCategoryId AND m.isPrimary = true " +
                            "AND m.isActive = true AND m.deleted = false",
                    SubCategoryMediaMapper.class)
                    .setParameter("subCategoryId", subCategoryId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<SubCategoryMediaMapper> findPrimaryBySubCategoryIds(List<Long> subCategoryIds) {
        if (subCategoryIds == null || subCategoryIds.isEmpty()) {
            return Collections.emptyList();
        }
        return em.createQuery(
                "SELECT m FROM SubCategoryMediaMapper m " +
                        "JOIN FETCH m.media med " +
                        "WHERE m.subCategory.id IN :subCategoryIds " +
                        "AND m.isPrimary = true AND m.isActive = true AND m.deleted = false " +
                        "AND med.isActive = true " +
                        "ORDER BY m.subCategory.id ASC, m.createdOn ASC",
                SubCategoryMediaMapper.class)
                .setParameter("subCategoryIds", subCategoryIds)
                .getResultList();
    }

    @Override
    public List<SubCategoryMediaMapper> findBySubCategoryIds(List<Long> subCategoryIds) {
        if (subCategoryIds == null || subCategoryIds.isEmpty()) {
            return Collections.emptyList();
        }
        return em.createQuery(
                "SELECT m FROM SubCategoryMediaMapper m " +
                        "JOIN FETCH m.media med " +
                        "WHERE m.subCategory.id IN :subCategoryIds " +
                        "AND m.isActive = true AND m.deleted = false AND med.isActive = true " +
                        "ORDER BY m.subCategory.id ASC, m.displayOrder ASC, m.createdOn ASC",
                SubCategoryMediaMapper.class)
                .setParameter("subCategoryIds", subCategoryIds)
                .getResultList();
    }
}
