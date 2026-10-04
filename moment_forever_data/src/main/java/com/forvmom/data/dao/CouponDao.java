package com.forvmom.data.dao;

import com.forvmom.data.entities.Coupon;

import java.util.List;
import java.util.Optional;

public interface CouponDao extends GenericDao<Coupon, Long> {
    Optional<Coupon> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<Coupon> searchCoupons(String query, Boolean activeOnly);
}