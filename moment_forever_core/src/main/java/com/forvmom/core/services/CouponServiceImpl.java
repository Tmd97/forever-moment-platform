package com.forvmom.core.services;

import com.forvmom.common.dto.request.CouponRequestDto;
import com.forvmom.common.dto.request.CouponValidationRequestDto;
import com.forvmom.common.dto.response.CouponResponseDto;
import com.forvmom.common.dto.response.CouponValidationResponseDto;
import com.forvmom.common.enums.DiscountType;
import com.forvmom.common.errorhandler.CustomAuthException;
import com.forvmom.common.errorhandler.ResourceNotFoundException;
import com.forvmom.core.mapper.CouponBeanMapper;
import com.forvmom.data.dao.CouponDao;
import com.forvmom.data.dao.ExperienceDao;
import com.forvmom.data.dao.ExperienceCouponMapperDao;
import com.forvmom.data.entities.Coupon;
import com.forvmom.data.entities.Experience;
import com.forvmom.data.entities.ExperienceCouponMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CouponServiceImpl implements CouponService {

    private static final Logger logger = LoggerFactory.getLogger(CouponServiceImpl.class);

    @Autowired
    private CouponDao couponDao;

    @Autowired
    private ExperienceDao experienceDao;

    @Autowired
    private ExperienceCouponMapperDao experienceCouponMapperDao;

    @Override
    @Transactional
    public CouponResponseDto createCoupon(CouponRequestDto dto) {
        String code = dto.getCode().trim().toUpperCase();
        if (couponDao.existsByCodeIgnoreCase(code)) {
            throw new CustomAuthException("Coupon code already exists: " + code);
        }

        Coupon coupon = CouponBeanMapper.mapDtoToEntity(dto);
        coupon.setCode(code);
        Coupon saved = couponDao.save(coupon);
        logger.info("Created coupon code: {}", saved.getCode());
        return CouponBeanMapper.mapEntityToDto(saved);
    }

    @Override
    @Transactional
    public CouponResponseDto updateCoupon(Long id, CouponRequestDto dto) {
        Coupon coupon = couponDao.findById(id);
        if (coupon == null) {
            throw new ResourceNotFoundException("Coupon not found with id: " + id);
        }

        String newCode = dto.getCode().trim().toUpperCase();
        if (!coupon.getCode().equalsIgnoreCase(newCode) && couponDao.existsByCodeIgnoreCase(newCode)) {
            throw new CustomAuthException("Coupon code already exists: " + newCode);
        }

        coupon.setCode(newCode);
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

        Coupon updated = couponDao.update(coupon);
        return CouponBeanMapper.mapEntityToDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponseDto getCouponById(Long id) {
        Coupon coupon = couponDao.findById(id);
        if (coupon == null) {
            throw new ResourceNotFoundException("Coupon not found with id: " + id);
        }
        return CouponBeanMapper.mapEntityToDto(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponseDto getCouponByCode(String code) {
        Coupon coupon = couponDao.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with code: " + code));
        return CouponBeanMapper.mapEntityToDto(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponseDto> searchCoupons(String query, Boolean activeOnly) {
        List<Coupon> coupons = couponDao.searchCoupons(query, activeOnly);
        return coupons.stream()
                .map(CouponBeanMapper::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = couponDao.findById(id);
        if (coupon == null) {
            throw new ResourceNotFoundException("Coupon not found with id: " + id);
        }
        couponDao.delete(coupon);
    }

    // ── Experience-Coupon Attachment ──────────────────────────────────────

    @Override
    @Transactional
    public List<CouponResponseDto> attachCouponsToExperience(Long experienceId, List<Long> couponIds) {
        Experience experience = experienceDao.findById(experienceId);
        if (experience == null) {
            throw new ResourceNotFoundException("Experience not found with id: " + experienceId);
        }

        experienceCouponMapperDao.deleteAllByExperienceId(experienceId);

        if (couponIds == null || couponIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<CouponResponseDto> result = new ArrayList<>();
        for (Long couponId : couponIds) {
            Coupon coupon = couponDao.findById(couponId);
            if (coupon == null) {
                throw new ResourceNotFoundException("Coupon not found with id: " + couponId);
            }
            ExperienceCouponMapper mapper = new ExperienceCouponMapper(experience, coupon);
            experienceCouponMapperDao.save(mapper);
            result.add(CouponBeanMapper.mapEntityToDto(coupon));
        }

        return result;
    }

    @Override
    @Transactional
    public void attachSingleCouponToExperience(Long experienceId, Long couponId) {
        Experience experience = experienceDao.findById(experienceId);
        if (experience == null) {
            throw new ResourceNotFoundException("Experience not found with id: " + experienceId);
        }
        Coupon coupon = couponDao.findById(couponId);
        if (coupon == null) {
            throw new ResourceNotFoundException("Coupon not found with id: " + couponId);
        }

        if (!experienceCouponMapperDao.existsByExperienceIdAndCouponId(experienceId, couponId)) {
            ExperienceCouponMapper mapper = new ExperienceCouponMapper(experience, coupon);
            experienceCouponMapperDao.save(mapper);
        }
    }

    @Override
    @Transactional
    public void detachCouponFromExperience(Long experienceId, Long couponId) {
        experienceCouponMapperDao.deleteByExperienceIdAndCouponId(experienceId, couponId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponseDto> getCouponsForExperience(Long experienceId) {
        List<ExperienceCouponMapper> mappers = experienceCouponMapperDao.findByExperienceId(experienceId);
        return mappers.stream()
                .map(ExperienceCouponMapper::getCoupon)
                .map(CouponBeanMapper::mapEntityToDto)
                .collect(Collectors.toList());
    }

    // ── Public Catalog & Validation ───────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponseDto> getActiveCouponsForExperience(Long experienceId) {
        List<Coupon> validCoupons = experienceCouponMapperDao.findValidCouponsForExperience(experienceId, LocalDate.now());
        return validCoupons.stream()
                .map(CouponBeanMapper::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResponseDto validateCoupon(CouponValidationRequestDto request) {
        CouponValidationResponseDto response = new CouponValidationResponseDto();
        response.setCouponCode(request.getCode());

        String code = request.getCode().trim().toUpperCase();
        Optional<Coupon> couponOptional = couponDao.findByCodeIgnoreCase(code);

        if (couponOptional.isEmpty()) {
            response.setValid(false);
            response.setMessage("Invalid coupon code");
            return response;
        }

        Coupon coupon = couponOptional.get();

        if (!coupon.isActive()) {
            response.setValid(false);
            response.setMessage("Coupon is inactive");
            return response;
        }

        LocalDate today = LocalDate.now();
        if (coupon.getValidFrom() != null && today.isBefore(coupon.getValidFrom())) {
            response.setValid(false);
            response.setMessage("Coupon is not valid yet");
            return response;
        }

        if (coupon.getValidTo() != null && today.isAfter(coupon.getValidTo())) {
            response.setValid(false);
            response.setMessage("Coupon has expired");
            return response;
        }

        if (coupon.getUsageLimit() != null && coupon.getUsageCount() >= coupon.getUsageLimit()) {
            response.setValid(false);
            response.setMessage("Coupon usage limit reached");
            return response;
        }

        // Check if coupon is mapped to the experience
        boolean mappedToExperience = experienceCouponMapperDao
                .existsByExperienceIdAndCouponId(request.getExperienceId(), coupon.getId());

        if (!mappedToExperience) {
            response.setValid(false);
            response.setMessage("Coupon is not applicable for this experience");
            return response;
        }

        // Check minimum booking amount constraint
        if (coupon.getMinBookingAmount() != null && request.getBookingAmount().compareTo(coupon.getMinBookingAmount()) < 0) {
            response.setValid(false);
            response.setMessage("Minimum booking amount of ₹" + coupon.getMinBookingAmount() + " required to use this coupon");
            return response;
        }

        // Calculate discount
        BigDecimal discount = BigDecimal.ZERO;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = request.getBookingAmount()
                    .multiply(coupon.getDiscountValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discount = coupon.getMaxDiscountAmount();
            }
        } else if (coupon.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discount = coupon.getDiscountValue();
            if (discount.compareTo(request.getBookingAmount()) > 0) {
                discount = request.getBookingAmount();
            }
        }

        BigDecimal finalAmount = request.getBookingAmount().subtract(discount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        response.setValid(true);
        response.setMessage("Coupon applied successfully! You saved ₹" + discount);
        response.setDiscountAmount(discount);
        response.setFinalAmount(finalAmount);
        return response;
    }
}