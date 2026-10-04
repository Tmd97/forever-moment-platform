package com.forvmom.core.controller.admin;

import com.forvmom.common.dto.request.CouponRequestDto;
import com.forvmom.common.dto.response.CouponResponseDto;
import com.forvmom.common.response.ApiResponse;
import com.forvmom.common.response.ResponseUtil;
import com.forvmom.common.utils.AppConstants;
import com.forvmom.core.services.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin API for managing promotional coupons and attaching them to experiences.
 */
@RestController
@RequestMapping("/admin/coupons")
@Tag(name = "Admin Coupon API", description = "Endpoints for managing coupons and experience attachments (Admin only)")
public class CouponControllerAdmin {

    @Autowired
    private CouponService couponService;

    @PostMapping
    @Operation(summary = "Create Coupon", description = "Creates a new promotional coupon code")
    public ResponseEntity<ApiResponse<?>> createCoupon(@Valid @RequestBody CouponRequestDto request) {
        CouponResponseDto response = couponService.createCoupon(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.buildCreatedResponse(response, AppConstants.MSG_CREATED));
    }

    @GetMapping
    @Operation(summary = "Search Coupons", description = "Lists coupons with optional search query and active filter")
    public ResponseEntity<ApiResponse<?>> searchCoupons(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean activeOnly) {
        List<CouponResponseDto> response = couponService.searchCoupons(query, activeOnly);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Coupon Details", description = "Retrieves details of a coupon by ID")
    public ResponseEntity<ApiResponse<?>> getCouponById(@PathVariable Long id) {
        CouponResponseDto response = couponService.getCouponById(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Coupon", description = "Updates details of an existing coupon")
    public ResponseEntity<ApiResponse<?>> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody CouponRequestDto request) {
        CouponResponseDto response = couponService.updateCoupon(id, request);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_UPDATED));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Coupon", description = "Soft-deletes a coupon")
    public ResponseEntity<ApiResponse<?>> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, AppConstants.MSG_DELETED));
    }

    // ── Experience-Coupon Attachments ────────────────────────────────────

    @GetMapping("/experiences/{experienceId}")
    @Operation(summary = "Get Coupons Attached to Experience", description = "Lists all coupons attached to a given experience")
    public ResponseEntity<ApiResponse<?>> getCouponsForExperience(@PathVariable Long experienceId) {
        List<CouponResponseDto> response = couponService.getCouponsForExperience(experienceId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_FETCHED));
    }

    @PutMapping("/experiences/{experienceId}")
    @Operation(summary = "Replace Coupons for Experience", description = "Bulk replaces/attaches a list of coupon IDs for an experience")
    public ResponseEntity<ApiResponse<?>> attachCouponsToExperience(
            @PathVariable Long experienceId,
            @RequestBody List<Long> couponIds) {
        List<CouponResponseDto> response = couponService.attachCouponsToExperience(experienceId, couponIds);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(response, AppConstants.MSG_UPDATED));
    }

    @PostMapping("/experiences/{experienceId}/attach/{couponId}")
    @Operation(summary = "Attach Single Coupon to Experience", description = "Attaches a specific coupon to an experience")
    public ResponseEntity<ApiResponse<?>> attachSingleCoupon(
            @PathVariable Long experienceId,
            @PathVariable Long couponId) {
        couponService.attachSingleCouponToExperience(experienceId, couponId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, AppConstants.MSG_UPDATED));
    }

    @DeleteMapping("/experiences/{experienceId}/detach/{couponId}")
    @Operation(summary = "Detach Coupon from Experience", description = "Removes a coupon attachment from an experience")
    public ResponseEntity<ApiResponse<?>> detachCoupon(
            @PathVariable Long experienceId,
            @PathVariable Long couponId) {
        couponService.detachCouponFromExperience(experienceId, couponId);
        return ResponseEntity.ok(ResponseUtil.buildOkResponse(null, AppConstants.MSG_DELETED));
    }
}