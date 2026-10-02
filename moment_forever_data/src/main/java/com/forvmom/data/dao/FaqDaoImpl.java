package com.forvmom.data.dao;

import com.forvmom.data.entities.Faq;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public class FaqDaoImpl extends GenericDaoImpl<Faq, Long> implements FaqDao {

    public FaqDaoImpl() {
        super(Faq.class);
    }

    @Override
    public List<Faq> findAllOrdered() {
        return em.createQuery(
                "SELECT f FROM Faq f WHERE f.deleted = false ORDER BY f.displayOrder ASC",
                Faq.class).getResultList();
    }

    @Override
    public List<Faq> findAllActiveOrdered() {
        return em.createQuery(
                "SELECT f FROM Faq f WHERE f.deleted = false AND f.isActive = true ORDER BY f.displayOrder ASC",
                Faq.class).getResultList();
    }

    @Override
    public boolean existsById(Long id) {
        Long count = em.createQuery(
                "SELECT COUNT(f) FROM Faq f WHERE f.id = :id AND f.deleted = false",
                Long.class).setParameter("id", id).getSingleResult();
        return count > 0;
    }

    @Override
    public Integer getMaxDisplayOrder() {
        Integer max = em.createQuery(
                "SELECT COALESCE(MAX(f.displayOrder), 0) FROM Faq f WHERE f.deleted = false",
                Integer.class).getSingleResult();
        return max != null ? max : 0;
    }
}
