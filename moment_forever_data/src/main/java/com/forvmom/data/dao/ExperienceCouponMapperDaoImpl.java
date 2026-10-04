package com.forvmom.data.dao;

import com.forvmom.data.entities.Coupon;
import com.forvmom.data.entities.ExperienceCouponMapper;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class ExperienceCouponMapperDaoImpl extends GenericDaoImpl<ExperienceCouponMapper, Long> implements ExperienceCouponMapperDao {

    public ExperienceCouponMapperDaoImpl() {
        super(ExperienceCouponMapper.class);
    }

    @Override
    public List<ExperienceCouponMapper> findByExperienceId(Long experienceId) {
        TypedQuery<ExperienceCouponMapper> query = em.createQuery(
                "SELECT m FROM ExperienceCouponMapper m JOIN FETCH m.coupon c " +
                        "WHERE m.experience.id = :experienceId AND m.deleted = false",
                ExperienceCouponMapper.class);
        query.setParameter("experienceId", experienceId);
        return query.getResultList();
    }

    @Override
    public List<ExperienceCouponMapper> findByCouponId(Long couponId) {
        TypedQuery<ExperienceCouponMapper> query = em.createQuery(
                "SELECT m FROM ExperienceCouponMapper m JOIN FETCH m.experience e " +
                        "WHERE m.coupon.id = :couponId AND m.deleted = false",
                ExperienceCouponMapper.class);
        query.setParameter("couponId", couponId);
        return query.getResultList();
    }

    @Override
    public Optional<ExperienceCouponMapper> findByExperienceIdAndCouponId(Long experienceId, Long couponId) {
        TypedQuery<ExperienceCouponMapper> query = em.createQuery(
                "SELECT m FROM ExperienceCouponMapper m " +
                        "WHERE m.experience.id = :experienceId AND m.coupon.id = :couponId AND m.deleted = false",
                ExperienceCouponMapper.class);
        query.setParameter("experienceId", experienceId);
        query.setParameter("couponId", couponId);
        try {
            return Optional.of(query.getSingleResult());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsByExperienceIdAndCouponId(Long experienceId, Long couponId) {
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(m) FROM ExperienceCouponMapper m " +
                        "WHERE m.experience.id = :experienceId AND m.coupon.id = :couponId AND m.deleted = false",
                Long.class);
        query.setParameter("experienceId", experienceId);
        query.setParameter("couponId", couponId);
        return query.getSingleResult() > 0;
    }

    @Override
    public void deleteAllByExperienceId(Long experienceId) {
        em.createQuery(
                "UPDATE ExperienceCouponMapper m SET m.deleted = true " +
                        "WHERE m.experience.id = :experienceId AND m.deleted = false")
                .setParameter("experienceId", experienceId)
                .executeUpdate();
    }

    @Override
    public void deleteByExperienceIdAndCouponId(Long experienceId, Long couponId) {
        em.createQuery(
                "UPDATE ExperienceCouponMapper m SET m.deleted = true " +
                        "WHERE m.experience.id = :experienceId AND m.coupon.id = :couponId AND m.deleted = false")
                .setParameter("experienceId", experienceId)
                .setParameter("couponId", couponId)
                .executeUpdate();
    }

    @Override
    public List<Coupon> findValidCouponsForExperience(Long experienceId, LocalDate currentDate) {
        TypedQuery<Coupon> query = em.createQuery(
                "SELECT DISTINCT c FROM ExperienceCouponMapper m JOIN m.coupon c " +
                        "WHERE m.experience.id = :experienceId AND m.isActive = true AND m.deleted = false " +
                        "AND c.isActive = true AND c.deleted = false " +
                        "AND (c.validFrom IS NULL OR c.validFrom <= :currentDate) " +
                        "AND (c.validTo IS NULL OR c.validTo >= :currentDate) " +
                        "AND (c.usageLimit IS NULL OR c.usageCount < c.usageLimit)",
                Coupon.class);
        query.setParameter("experienceId", experienceId);
        query.setParameter("currentDate", currentDate);
        return query.getResultList();
    }
}