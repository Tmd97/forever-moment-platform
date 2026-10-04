package com.forvmom.core.services;

import com.forvmom.common.dto.request.CouponRequestDto;
import com.forvmom.common.dto.request.CouponValidationRequestDto;
import com.forvmom.common.dto.response.CouponResponseDto;
import com.forvmom.common.dto.response.CouponValidationResponseDto;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {
    // Coupon Master CRUD (Admin)
    CouponResponseDto createCoupon(CouponRequestDto dto);
    CouponResponseDto updateCoupon(Long id, CouponRequestDto dto);
    CouponResponseDto getCouponById(Long id);
    CouponResponseDto getCouponByCode(String code);
    List<CouponResponseDto> searchCoupons(String query, Boolean activeOnly);
    void deleteCoupon(Long id);

    // Experience-Coupon Mapping (Admin)
    List<CouponResponseDto> attachCouponsToExperience(Long experienceId, List<Long> couponIds);
    void attachSingleCouponToExperience(Long experienceId, Long couponId);
    void detachCouponFromExperience(Long experienceId, Long couponId);
    List<CouponResponseDto> getCouponsForExperience(Long experienceId);

    // Public / Customer Catalog & Validation
    List<CouponResponseDto> getActiveCouponsForExperience(Long experienceId);
    CouponValidationResponseDto validateCoupon(CouponValidationRequestDto request);
}