package com.forvmom.common.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class CouponValidationRequestDto {

    @NotBlank(message = "Coupon code is required")
    private String code;

    @NotNull(message = "Experience ID is required")
    private Long experienceId;

    @NotNull(message = "Booking amount is required")
    @Positive(message = "Booking amount must be positive")
    private BigDecimal bookingAmount;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getExperienceId() {
        return experienceId;
    }

    public void setExperienceId(Long experienceId) {
        this.experienceId = experienceId;
    }

    public BigDecimal getBookingAmount() {
        return bookingAmount;
    }

    public void setBookingAmount(BigDecimal bookingAmount) {
        this.bookingAmount = bookingAmount;
    }
}