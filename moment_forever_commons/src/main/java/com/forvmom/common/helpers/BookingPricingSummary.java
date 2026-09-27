package com.forvmom.common.helpers;

import com.forvmom.common.dto.events.BookingRequestEvent.BookedAddonSnapshot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BookingPricingSummary {

    private final BigDecimal resolvedPricePerPerson;
    private final String pricingLevel;
    private final BigDecimal totalAmount;
    private final BigDecimal addonsTotal;
    private final BigDecimal grandTotal;
    private final List<BookedAddonSnapshot> bookedAddonSnapshots;

    private BookingPricingSummary(Builder builder) {
        this.resolvedPricePerPerson = builder.resolvedPricePerPerson;
        this.pricingLevel = builder.pricingLevel;
        this.totalAmount = builder.totalAmount;
        this.addonsTotal = builder.addonsTotal;
        this.grandTotal = builder.grandTotal;
        this.bookedAddonSnapshots = builder.bookedAddonSnapshots == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(builder.bookedAddonSnapshots));
    }

    public BigDecimal getResolvedPricePerPerson() {
        return resolvedPricePerPerson;
    }

    public String getPricingLevel() {
        return pricingLevel;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getAddonsTotal() {
        return addonsTotal;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public List<BookedAddonSnapshot> getBookedAddonSnapshots() {
        return bookedAddonSnapshots;
    }

    public static class Builder {

        private BigDecimal resolvedPricePerPerson;
        private String pricingLevel;
        private BigDecimal totalAmount;
        private BigDecimal addonsTotal;
        private BigDecimal grandTotal;
        private List<BookedAddonSnapshot> bookedAddonSnapshots;

        public Builder withResolvedPricePerPerson(BigDecimal resolvedPricePerPerson) {
            this.resolvedPricePerPerson = resolvedPricePerPerson;
            return this;
        }

        public Builder withPricingLevel(String pricingLevel) {
            this.pricingLevel = pricingLevel;
            return this;
        }

        public Builder withTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public Builder withAddonsTotal(BigDecimal addonsTotal) {
            this.addonsTotal = addonsTotal;
            return this;
        }

        public Builder withGrandTotal(BigDecimal grandTotal) {
            this.grandTotal = grandTotal;
            return this;
        }

        public Builder withBookedAddonSnapshots(List<BookedAddonSnapshot> bookedAddonSnapshots) {
            this.bookedAddonSnapshots = bookedAddonSnapshots;
            return this;
        }

        public BookingPricingSummary build() {
            validate();
            return new BookingPricingSummary(this);
        }

        private void validate() {
            if (resolvedPricePerPerson == null) {
                throw new IllegalStateException("resolvedPricePerPerson is required");
            }

            if (pricingLevel == null) {
                throw new IllegalStateException("pricingLevel is required");
            }

            if (totalAmount == null) {
                throw new IllegalStateException("totalAmount is required");
            }

            if (addonsTotal == null) {
                throw new IllegalStateException("addonsTotal is required");
            }

            if (grandTotal == null) {
                throw new IllegalStateException("grandTotal is required");
            }
        }
    }
}