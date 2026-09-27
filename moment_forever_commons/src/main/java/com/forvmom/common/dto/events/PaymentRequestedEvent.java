package com.forvmom.common.dto.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Published to topic {@code payment-requested} by the Booking Service
 * immediately after persisting a PENDING booking.
 * Consumed by the Payment Service (future).
 */
public class PaymentRequestedEvent extends BaseEvent {

    private String bookingId;
    private Long userId;
    private String userEmail;
    private BigDecimal grandTotal;
    /** ISO 4217 currency code, default INR */
    private String currency = "INR";
    private LocalDateTime requestedAt;

    public PaymentRequestedEvent() {
        super();
        setEventType("PAYMENT_REQUESTED");
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }
}
