package com.forvmom.common.helpers;

import com.forvmom.common.dto.snapshot.AddonSnapshot;
import com.forvmom.common.dto.snapshot.ExperienceSnapshot;
import com.forvmom.common.dto.snapshot.LocationSnapshot;
import com.forvmom.common.dto.snapshot.SlotSnapshot;
import com.forvmom.common.dto.snapshot.UserSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BookingSnapshotBundle {

    private final UserSnapshot userSnapshot;
    private final SlotSnapshot slotSnapshot;
    private final ExperienceSnapshot experienceSnapshot;
    private final LocationSnapshot locationSnapshot;
    private final List<AddonSnapshot> addonSnapshots;

    private BookingSnapshotBundle(Builder builder) {
        this.userSnapshot = builder.userSnapshot;
        this.slotSnapshot = builder.slotSnapshot;
        this.experienceSnapshot = builder.experienceSnapshot;
        this.locationSnapshot = builder.locationSnapshot;
        this.addonSnapshots = builder.addonSnapshots == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(builder.addonSnapshots));
    }

    public UserSnapshot getUserSnapshot() {
        return userSnapshot;
    }

    public SlotSnapshot getSlotSnapshot() {
        return slotSnapshot;
    }

    public ExperienceSnapshot getExperienceSnapshot() {
        return experienceSnapshot;
    }

    public LocationSnapshot getLocationSnapshot() {
        return locationSnapshot;
    }

    public List<AddonSnapshot> getAddonSnapshots() {
        return addonSnapshots;
    }

    public static class Builder {

        private UserSnapshot userSnapshot;
        private SlotSnapshot slotSnapshot;
        private ExperienceSnapshot experienceSnapshot;
        private LocationSnapshot locationSnapshot;
        private List<AddonSnapshot> addonSnapshots;

        public Builder withUserSnapshot(UserSnapshot userSnapshot) {
            this.userSnapshot = userSnapshot;
            return this;
        }

        public Builder withSlotSnapshot(SlotSnapshot slotSnapshot) {
            this.slotSnapshot = slotSnapshot;
            return this;
        }

        public Builder withExperienceSnapshot(ExperienceSnapshot experienceSnapshot) {
            this.experienceSnapshot = experienceSnapshot;
            return this;
        }

        public Builder withLocationSnapshot(LocationSnapshot locationSnapshot) {
            this.locationSnapshot = locationSnapshot;
            return this;
        }

        public Builder withAddonSnapshots(List<AddonSnapshot> addonSnapshots) {
            this.addonSnapshots = addonSnapshots;
            return this;
        }

        public BookingSnapshotBundle build() {
            validate();
            return new BookingSnapshotBundle(this);
        }

        private void validate() {
            if (userSnapshot == null) {
                throw new IllegalStateException("userSnapshot is required");
            }

            if (slotSnapshot == null) {
                throw new IllegalStateException("slotSnapshot is required");
            }

            if (experienceSnapshot == null) {
                throw new IllegalStateException("experienceSnapshot is required");
            }

            if (locationSnapshot == null) {
                throw new IllegalStateException("locationSnapshot is required");
            }
        }
    }
}