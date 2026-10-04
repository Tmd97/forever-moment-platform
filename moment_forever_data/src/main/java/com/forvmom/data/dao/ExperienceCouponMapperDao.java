package com.forvmom.data.dao;

import com.forvmom.data.entities.Coupon;
import com.forvmom.data.entities.ExperienceCouponMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExperienceCouponMapperDao extends GenericDao<ExperienceCouponMapper, Long> {
    List<ExperienceCouponMapper> findByExperienceId(Long experienceId);
    List<ExperienceCouponMapper> findByCouponId(Long couponId);
    Optional<ExperienceCouponMapper> findByExperienceIdAndCouponId(Long experienceId, Long couponId);
    boolean existsByExperienceIdAndCouponId(Long experienceId, Long couponId);
    void deleteAllByExperienceId(Long experienceId);
    void deleteByExperienceIdAndCouponId(Long experienceId, Long couponId);
    List<Coupon> findValidCouponsForExperience(Long experienceId, LocalDate currentDate);
}