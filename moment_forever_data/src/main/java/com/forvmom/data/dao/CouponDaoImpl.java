package com.forvmom.data.dao;

import com.forvmom.data.entities.Coupon;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class CouponDaoImpl extends GenericDaoImpl<Coupon, Long> implements CouponDao {

    public CouponDaoImpl() {
        super(Coupon.class);
    }

    @Override
    public Optional<Coupon> findByCodeIgnoreCase(String code) {
        TypedQuery<Coupon> query = em.createQuery(
                "SELECT c FROM Coupon c WHERE LOWER(c.code) = LOWER(:code)",
                Coupon.class);
        query.setParameter("code", code);
        try {
            return Optional.of(query.getSingleResult());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsByCodeIgnoreCase(String code) {
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(c) FROM Coupon c WHERE LOWER(c.code) = LOWER(:code)",
                Long.class);
        query.setParameter("code", code);
        return query.getSingleResult() > 0;
    }

    @Override
    public List<Coupon> searchCoupons(String searchTerm, Boolean activeOnly) {
        StringBuilder jpql = new StringBuilder("SELECT c FROM Coupon c WHERE 1=1 ");

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            jpql.append("AND (LOWER(c.code) LIKE :term OR LOWER(c.name) LIKE :term OR LOWER(c.description) LIKE :term) ");
        }

        if (Boolean.TRUE.equals(activeOnly)) {
            jpql.append("AND c.isActive = true ");
        }

        jpql.append("ORDER BY c.id DESC");

        TypedQuery<Coupon> query = em.createQuery(jpql.toString(), Coupon.class);

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            query.setParameter("term", "%" + searchTerm.trim().toLowerCase() + "%");
        }

        return query.getResultList();
    }
}