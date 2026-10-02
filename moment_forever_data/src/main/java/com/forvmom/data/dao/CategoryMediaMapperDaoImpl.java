package com.forvmom.data.dao;

import com.forvmom.data.entities.CategoryMediaMapper;
import jakarta.persistence.NoResultException;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class CategoryMediaMapperDaoImpl extends GenericDaoImpl<CategoryMediaMapper, Long>
        implements CategoryMediaMapperDao {

    public CategoryMediaMapperDaoImpl() {
        super(CategoryMediaMapper.class);
    }

    @Override
    public List<CategoryMediaMapper> findByCategoryId(Long categoryId) {
        return em.createQuery(
                "SELECT m FROM CategoryMediaMapper m " +
                        "JOIN FETCH m.media med " +
                        "WHERE m.category.id = :categoryId AND m.deleted = false " +
                        "ORDER BY m.displayOrder ASC, m.createdOn ASC",
                CategoryMediaMapper.class)
                .setParameter("categoryId", categoryId)
                .getResultList();
    }

    @Override
    public boolean existsByCategoryIdAndMediaId(Long categoryId, Long mediaId) {
        Long count = em.createQuery(
                "SELECT COUNT(m) FROM CategoryMediaMapper m " +
                        "WHERE m.category.id = :categoryId AND m.media.id = :mediaId AND m.deleted = false",
                Long.class)
                .setParameter("categoryId", categoryId)
                .setParameter("mediaId", mediaId)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public CategoryMediaMapper findByCategoryIdAndMediaId(Long categoryId, Long mediaId) {
        try {
            return em.createQuery(
                    "SELECT m FROM CategoryMediaMapper m " +
                            "JOIN FETCH m.media med " +
                            "WHERE m.category.id = :categoryId AND m.media.id = :mediaId AND m.deleted = false",
                    CategoryMediaMapper.class)
                    .setParameter("categoryId", categoryId)
                    .setParameter("mediaId", mediaId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public CategoryMediaMapper findPrimaryByCategoryId(Long categoryId) {
        try {
            return em.createQuery(
                    "SELECT m FROM CategoryMediaMapper m " +
                            "JOIN FETCH m.media med " +
                            "WHERE m.category.id = :categoryId AND m.isPrimary = true " +
                            "AND m.isActive = true AND m.deleted = false",
                    CategoryMediaMapper.class)
                    .setParameter("categoryId", categoryId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<CategoryMediaMapper> findPrimaryByCategoryIds(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Collections.emptyList();
        }
        return em.createQuery(
                "SELECT m FROM CategoryMediaMapper m " +
                        "JOIN FETCH m.media med " +
                        "WHERE m.category.id IN :categoryIds " +
                        "AND m.isPrimary = true AND m.isActive = true AND m.deleted = false " +
                        "AND med.isActive = true " +
                        "ORDER BY m.category.id ASC, m.createdOn ASC",
                CategoryMediaMapper.class)
                .setParameter("categoryIds", categoryIds)
                .getResultList();
    }

    @Override
    public List<CategoryMediaMapper> findByCategoryIds(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Collections.emptyList();
        }
        return em.createQuery(
                "SELECT m FROM CategoryMediaMapper m " +
                        "JOIN FETCH m.media med " +
                        "WHERE m.category.id IN :categoryIds " +
                        "AND m.isActive = true AND m.deleted = false AND med.isActive = true " +
                        "ORDER BY m.category.id ASC, m.displayOrder ASC, m.createdOn ASC",
                CategoryMediaMapper.class)
                .setParameter("categoryIds", categoryIds)
                .getResultList();
    }
}
