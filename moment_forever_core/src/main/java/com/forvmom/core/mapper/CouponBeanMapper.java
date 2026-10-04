package com.forvmom.core.mapper;

import com.forvmom.common.dto.request.CouponRequestDto;
import com.forvmom.common.dto.response.CouponResponseDto;
import com.forvmom.data.entities.Coupon;

public class CouponBeanMapper {

    public static CouponResponseDto mapEntityToDto(Coupon coupon) {
        if (coupon == null) {
            return null;
        }
        CouponResponseDto dto = new CouponResponseDto();
        dto.setId(coupon.getId());
        dto.setCode(coupon.getCode());
        dto.setName(coupon.getName());
        dto.setDescription(coupon.getDescription());
        dto.setDiscountType(coupon.getDiscountType());
        dto.setDiscountValue(coupon.getDiscountValue());
        dto.setMaxDiscountAmount(coupon.getMaxDiscountAmount());
        dto.setMinBookingAmount(coupon.getMinBookingAmount());
        dto.setValidFrom(coupon.getValidFrom());
        dto.setValidTo(coupon.getValidTo());
        dto.setUsageLimit(coupon.getUsageLimit());
        dto.setUsageCount(coupon.getUsageCount());
        dto.setIsActive(coupon.isActive());
        dto.setCreatedOn(coupon.getCreatedOn());
        dto.setUpdatedOn(coupon.getUpdatedOn());
        return dto;
    }

    public static Coupon mapDtoToEntity(CouponRequestDto dto) {
        if (dto == null) {
            return null;
        }
        Coupon coupon = new Coupon();
        coupon.setCode(dto.getCode() != null ? dto.getCode().trim().toUpperCase() : null);
        coupon.setName(dto.getName());
        coupon.setDescription(dto.getDescription());
        coupon.setDiscountType(dto.getDiscountType());
        coupon.setDiscountValue(dto.getDiscountValue());
        coupon.setMaxDiscountAmount(dto.getMaxDiscountAmount());
        coupon.setMinBookingAmount(dto.getMinBookingAmount());
        coupon.setValidFrom(dto.getValidFrom());
        coupon.setValidTo(dto.getValidTo());
        coupon.setUsageLimit(dto.getUsageLimit());
        if (dto.getIsActive() != null) {
            coupon.setActive(dto.getIsActive());
        }
        return coupon;
    }
}