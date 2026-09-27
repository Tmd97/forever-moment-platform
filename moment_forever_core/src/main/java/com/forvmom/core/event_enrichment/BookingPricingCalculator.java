package com.forvmom.core.event_enrichment;

import com.forvmom.common.dto.events.BookingRequestEvent.BookedAddonSnapshot;
import com.forvmom.common.dto.snapshot.AddonSnapshot;
import com.forvmom.common.helpers.BookingOutboxPayload;
import com.forvmom.common.helpers.BookingPricingSummary;
import com.forvmom.common.helpers.BookingSnapshotBundle;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookingPricingCalculator {

    private static final String PRICING_LEVEL_BASE = "BASE";
    private static final String PRICING_LEVEL_LOCATION = "LOCATION";
    private static final String PRICING_LEVEL_SLOT = "SLOT";

    public BookingPricingSummary calculate(
            BookingOutboxPayload payload,
            BookingSnapshotBundle snapshots
    ) {
        BigDecimal resolvedPrice = snapshots
                .getExperienceSnapshot()
                .getBasePrice();

        String pricingLevel = PRICING_LEVEL_BASE;

        if (snapshots.getLocationSnapshot().getPriceOverride() != null) {
            resolvedPrice = snapshots.getLocationSnapshot().getPriceOverride();
            pricingLevel = PRICING_LEVEL_LOCATION;
        }

        if (snapshots.getSlotSnapshot().getPriceOverride() != null) {
            resolvedPrice = snapshots.getSlotSnapshot().getPriceOverride();
            pricingLevel = PRICING_LEVEL_SLOT;
        }

        BigDecimal totalAmount = resolvedPrice.multiply(
                BigDecimal.valueOf(payload.getGuestCount())
        );

        List<BookedAddonSnapshot> bookedAddonSnapshots = new ArrayList<>();
        BigDecimal addonsTotal = BigDecimal.ZERO;

        for (AddonSnapshot addonSnapshot : snapshots.getAddonSnapshots()) {
            bookedAddonSnapshots.add(new BookedAddonSnapshot(
                    addonSnapshot.getAddonMapperId(),
                    addonSnapshot.getAddonName(),
                    addonSnapshot.getEffectivePrice(),
                    addonSnapshot.isFree()
            ));

            addonsTotal = addonsTotal.add(addonSnapshot.getEffectivePrice());
        }

        BigDecimal grandTotal = totalAmount.add(addonsTotal);

        return new BookingPricingSummary.Builder()
                .withResolvedPricePerPerson(resolvedPrice)
                .withPricingLevel(pricingLevel)
                .withTotalAmount(totalAmount)
                .withAddonsTotal(addonsTotal)
                .withGrandTotal(grandTotal)
                .withBookedAddonSnapshots(bookedAddonSnapshots)
                .build();
    }
}