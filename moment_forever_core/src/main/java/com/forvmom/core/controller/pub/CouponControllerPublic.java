package com.forvmom.core.controller.pub;

import com.forvmom.common.dto.request.CouponValidationRequestDto;
import com.forvmom.common.dto.response.CouponResponseDto;
import com.forvmom.common.dto.response.CouponValidationResponseDto;
import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.common.utils.AppConstants;
import com.forvmom.core.services.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public catalog & validation endpoints for coupons.
 */
@RestController
@RequestMapping("/public/coupons")
@Tag(name = "Public Coupon API", description = "Public endpoints for checking applicable coupons and validating coupon codes")
public class CouponControllerPublic {

    @Autowired
    private CouponService couponService;

    @GetMapping("/experiences/{experienceId}")
    @Operation(summary = "Get Active Coupons for Experience", description = "Lists active and valid promotional coupons applicable to an experience")
    public ResponseEntity<ApiResponse<?>> getActiveCouponsForExperience(@PathVariable Long experienceId) {
        List<CouponResponseDto> response = couponService.getActiveCouponsForExperience(experienceId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate Coupon Code", description = "Validates a coupon code against an experience and booking amount, returning discount calculation")
    public ResponseEntity<ApiResponse<?>> validateCoupon(@Valid @RequestBody CouponValidationRequestDto request) {
        CouponValidationResponseDto response = couponService.validateCoupon(request);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_SUCCESS));
    }
}