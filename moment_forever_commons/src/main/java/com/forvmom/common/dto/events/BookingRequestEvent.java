package com.forvmom.common.dto.events;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Kafka event published to {@code booking-requested} topic when a user
 * initiates a booking.
 *
 * <p>
 * This payload is self-contained — the booking microservice should never need
 * to
 * call back to the core service to resolve pricing or validate add-ons.
 *
 * <p>
 * Pricing hierarchy (resolved before publishing):
 * <ol>
 * <li>BASE — {@code Experience.base_price}</li>
 * <li>LOCATION — {@code ExperienceLocationMapper.price_override}</li>
 * <li>SLOT — {@code ExperienceTimeSlotMapper.price_override}</li>
 * <li>PINCODE — {@code ExperiencePincodePrice.price_override} (optional,
 * future)</li>
 * </ol>
 */
public class BookingRequestEvent extends BaseEvent {

    // ── Booking identity ─────────────────────────────────────────────────────

    /** Generated booking reference, e.g. {@code MFB-1735000000000-a3f2}. */
    private String bookingId;

    // ── User snapshot ────────────────────────────────────────────────────────

    private Long userId;
    private String userEmail;
    private String userFullName;

    // ── Experience snapshot ───────────────────────────────────────────────────

    private Long experienceId;
    private String experienceName;
    private String experienceSlug;

    // ── Location snapshot ─────────────────────────────────────────────────────

    private Long locationId;
    private String locationName;

    // ── Slot snapshot ─────────────────────────────────────────────────────────

    /** {@code ExperienceTimeSlotMapper.id} — used for inventory update. */
    private Long timeSlotMapperId;
    private Long timeSlotId;
    private String timeSlotLabel;
    private String startTime;
    private String endTime;

    // ── Booking details ───────────────────────────────────────────────────────

    private LocalDate bookingDate;
    private Integer guestCount;

    /**
     * Optional pincode selected by user. {@code null} if not provided.
     * Carried for audit / display, but price was already resolved by core.
     */
    private String pincode;

    // ── Pricing (fully resolved before publishing) ────────────────────────────

    /** The final per-person price after applying the pricing chain. */
    private BigDecimal resolvedPricePerPerson;

    /**
     * Which level of the pricing chain produced {@code resolvedPricePerPerson}.
     * Values: {@code BASE}, {@code LOCATION}, {@code SLOT}, {@code PINCODE}.
     */
    private String pricingLevel;

    /** {@code resolvedPricePerPerson × guestCount} */
    private BigDecimal totalAmount;

    // ── Add-ons ───────────────────────────────────────────────────────────────

    /** Resolved add-on items with name and effective price. */
    private List<BookedAddonSnapshot> addons;

    /** Sum of {@code effectivePrice} for all non-free add-ons. */
    private BigDecimal addonsTotal;

    /** {@code totalAmount + addonsTotal} */
    private BigDecimal grandTotal;

    // ── Metadata ──────────────────────────────────────────────────────────────

    private LocalDateTime requestedAt;

    // ── Constructor ───────────────────────────────────────────────────────────

    public BookingRequestEvent() {
        super();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

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

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public Long getExperienceId() {
        return experienceId;
    }

    public void setExperienceId(Long experienceId) {
        this.experienceId = experienceId;
    }

    public String getExperienceName() {
        return experienceName;
    }

    public void setExperienceName(String experienceName) {
        this.experienceName = experienceName;
    }

    public String getExperienceSlug() {
        return experienceSlug;
    }

    public void setExperienceSlug(String experienceSlug) {
        this.experienceSlug = experienceSlug;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public Long getTimeSlotMapperId() {
        return timeSlotMapperId;
    }

    public void setTimeSlotMapperId(Long timeSlotMapperId) {
        this.timeSlotMapperId = timeSlotMapperId;
    }

    public Long getTimeSlotId() {
        return timeSlotId;
    }

    public void setTimeSlotId(Long timeSlotId) {
        this.timeSlotId = timeSlotId;
    }

    public String getTimeSlotLabel() {
        return timeSlotLabel;
    }

    public void setTimeSlotLabel(String timeSlotLabel) {
        this.timeSlotLabel = timeSlotLabel;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public Integer getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(Integer guestCount) {
        this.guestCount = guestCount;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public BigDecimal getResolvedPricePerPerson() {
        return resolvedPricePerPerson;
    }

    public void setResolvedPricePerPerson(BigDecimal resolvedPricePerPerson) {
        this.resolvedPricePerPerson = resolvedPricePerPerson;
    }

    public String getPricingLevel() {
        return pricingLevel;
    }

    public void setPricingLevel(String pricingLevel) {
        this.pricingLevel = pricingLevel;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<BookedAddonSnapshot> getAddons() {
        return addons;
    }

    public void setAddons(List<BookedAddonSnapshot> addons) {
        this.addons = addons;
    }

    public BigDecimal getAddonsTotal() {
        return addonsTotal;
    }

    public void setAddonsTotal(BigDecimal addonsTotal) {
        this.addonsTotal = addonsTotal;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    // ── Inner class: Add-on snapshot ──────────────────────────────────────────

    /**
     * Immutable snapshot of a single add-on as it was priced at booking time.
     */
    public static class BookedAddonSnapshot {

        /** {@code ExperienceAddonMapper.id} */
        private Long addonMapperId;
        private String addonName;

        /** Resolved price: 0 if free, priceOverride if set, else Addon.basePrice */
        private BigDecimal effectivePrice;
        private boolean free;

        public BookedAddonSnapshot() {
        }

        public BookedAddonSnapshot(Long addonMapperId, String addonName,
                BigDecimal effectivePrice, boolean free) {
            this.addonMapperId = addonMapperId;
            this.addonName = addonName;
            this.effectivePrice = effectivePrice;
            this.free = free;
        }

        public Long getAddonMapperId() {
            return addonMapperId;
        }

        public void setAddonMapperId(Long addonMapperId) {
            this.addonMapperId = addonMapperId;
        }

        public String getAddonName() {
            return addonName;
        }

        public void setAddonName(String addonName) {
            this.addonName = addonName;
        }

        public BigDecimal getEffectivePrice() {
            return effectivePrice;
        }

        public void setEffectivePrice(BigDecimal effectivePrice) {
            this.effectivePrice = effectivePrice;
        }

        public boolean isFree() {
            return free;
        }

        public void setFree(boolean free) {
            this.free = free;
        }
    }
}
