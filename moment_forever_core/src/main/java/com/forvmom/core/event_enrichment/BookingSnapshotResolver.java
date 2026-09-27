package com.forvmom.core.event_enrichment;

import com.forvmom.common.dto.snapshot.AddonSnapshot;
import com.forvmom.common.dto.snapshot.ExperienceSnapshot;
import com.forvmom.common.dto.snapshot.LocationSnapshot;
import com.forvmom.common.dto.snapshot.SlotSnapshot;
import com.forvmom.common.dto.snapshot.UserSnapshot;
import com.forvmom.common.helpers.BookingOutboxPayload;
import com.forvmom.common.helpers.BookingSnapshotBundle;
import com.forvmom.core.services.CatalogCacheService;
import com.forvmom.data.dao.ApplicationUserDao;
import com.forvmom.data.dao.ExperienceAddonMapperDao;
import com.forvmom.data.dao.ExperienceTimeSlotMapperDao;
import com.forvmom.data.entities.ApplicationUser;
import com.forvmom.data.entities.Experience;
import com.forvmom.data.entities.ExperienceAddonMapper;
import com.forvmom.data.entities.ExperienceLocationMapper;
import com.forvmom.data.entities.ExperienceTimeSlotMapper;
import com.forvmom.data.entities.Location;
import com.forvmom.data.entities.TimeSlot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class BookingSnapshotResolver {

    private final CatalogCacheService catalogCache;
    private final ApplicationUserDao userDao;
    private final ExperienceTimeSlotMapperDao slotMapperDao;
    private final ExperienceAddonMapperDao addonMapperDao;

    public BookingSnapshotResolver(
            CatalogCacheService catalogCache,
            ApplicationUserDao userDao,
            ExperienceTimeSlotMapperDao slotMapperDao,
            ExperienceAddonMapperDao addonMapperDao
    ) {
        this.catalogCache = catalogCache;
        this.userDao = userDao;
        this.slotMapperDao = slotMapperDao;
        this.addonMapperDao = addonMapperDao;
    }

    // TO DO: we will replace @Transactional as multiple time for lazy load we hit the DB
    // and on top of that we are  looking for redis as well. So we need to optimize this code to reduce the number of DB hits.
    // Best solution: use entity graph to fetch all the required data in one go and then use that data to create the snapshot bundle.

    @Transactional
    public BookingSnapshotBundle resolve(BookingOutboxPayload payload) {
        UserSnapshot userSnapshot = resolveUser(payload.getUserId());

        SlotSnapshot slotSnapshot = resolveSlot(payload.getSlotMapperId());

        ExperienceSnapshot experienceSnapshot = resolveExperience(
                payload.getSlotMapperId(),
                slotSnapshot.getExperienceId()
        );

        LocationSnapshot locationSnapshot = resolveLocation(
                payload.getSlotMapperId(),
                slotSnapshot.getExperienceId(),
                slotSnapshot.getLocationId()
        );

        List<AddonSnapshot> addonSnapshots = resolveAddons(payload.getAddonMapperIds());

        return new BookingSnapshotBundle.Builder()
                .withUserSnapshot(userSnapshot)
                .withSlotSnapshot(slotSnapshot)
                .withExperienceSnapshot(experienceSnapshot)
                .withLocationSnapshot(locationSnapshot)
                .withAddonSnapshots(addonSnapshots)
                .build();
    }

    private UserSnapshot resolveUser(Long userId) {
        UserSnapshot cachedUser = catalogCache.getUserSnapshot(userId);

        if (cachedUser != null) {
            return cachedUser;
        }

        ApplicationUser user = userDao.findById(userId);

        if (user == null) {
            throw new IllegalStateException("User not found: " + userId);
        }

        catalogCache.warmUserCache(user);

        return new UserSnapshot(
                user.getId(),
                user.getEmail(),
                user.getFullName()
        );
    }

    private SlotSnapshot resolveSlot(Long slotMapperId) {
        SlotSnapshot cachedSlot = catalogCache.getSlotSnapshot(slotMapperId);

        if (cachedSlot != null) {
            return cachedSlot;
        }

        ExperienceTimeSlotMapper slotMapper = slotMapperDao.findById(slotMapperId);

        if (slotMapper == null) {
            throw new IllegalStateException("SlotMapper not found: " + slotMapperId);
        }

        catalogCache.warmSlotCache(slotMapper);

        ExperienceLocationMapper experienceLocation = slotMapper.getExperienceLocation();
        Experience experience = experienceLocation.getExperience();
        Location location = experienceLocation.getLocation();
        TimeSlot timeSlot = slotMapper.getTimeSlot();

        return new SlotSnapshot(
                slotMapper.getId(),
                experience.getId(),
                location.getId(),
                timeSlot.getId(),
                timeSlot.getLabel(),
                timeSlot.getStartTime() != null ? timeSlot.getStartTime().toString() : null,
                timeSlot.getEndTime() != null ? timeSlot.getEndTime().toString() : null,
                slotMapper.getPriceOverride(),
                slotMapper.getMaxCapacity()
        );
    }

    private ExperienceSnapshot resolveExperience(
            Long slotMapperId,
            Long experienceId
    ) {
        ExperienceSnapshot cachedExperience = catalogCache.getExperienceSnapshot(experienceId);

        if (cachedExperience != null) {
            return cachedExperience;
        }

        ExperienceTimeSlotMapper slotMapper = slotMapperDao.findById(slotMapperId);

        if (slotMapper == null) {
            throw new IllegalStateException("SlotMapper not found: " + slotMapperId);
        }

        Experience experience = slotMapper
                .getExperienceLocation()
                .getExperience();

        catalogCache.warmExperienceCache(experience);

        return new ExperienceSnapshot(
                experience.getId(),
                experience.getName(),
                experience.getSlug(),
                experience.getBasePrice()
        );
    }

    private LocationSnapshot resolveLocation(
            Long slotMapperId,
            Long experienceId,
            Long locationId
    ) {
        LocationSnapshot cachedLocation = catalogCache.getLocationSnapshot(
                experienceId,
                locationId
        );

        if (cachedLocation != null) {
            return cachedLocation;
        }

        ExperienceTimeSlotMapper slotMapper = slotMapperDao.findById(slotMapperId);

        if (slotMapper == null) {
            throw new IllegalStateException("SlotMapper not found: " + slotMapperId);
        }

        ExperienceLocationMapper experienceLocation = slotMapper.getExperienceLocation();
        Location location = experienceLocation.getLocation();

        catalogCache.warmLocationCache(experienceLocation);

        return new LocationSnapshot(
                location.getId(),
                location.getName(),
                experienceLocation.getPriceOverride()
        );
    }

    private List<AddonSnapshot> resolveAddons(List<Long> addonMapperIds) {
        if (addonMapperIds == null || addonMapperIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<AddonSnapshot> result = new ArrayList<>();

        for (Long addonMapperId : addonMapperIds) {
            AddonSnapshot addonSnapshot = catalogCache.getAddonSnapshot(addonMapperId);

            if (addonSnapshot == null) {
                ExperienceAddonMapper addonMapper = addonMapperDao.findById(addonMapperId);

                if (addonMapper == null) {
                    throw new IllegalStateException("AddonMapper not found: " + addonMapperId);
                }

                catalogCache.warmAddonCache(addonMapper);

                addonSnapshot = new AddonSnapshot(
                        addonMapperId,
                        addonMapper.getAddon().getName(),
                        addonMapper.effectivePrice(),
                        Boolean.TRUE.equals(addonMapper.getIsFree())
                );
            }

            result.add(addonSnapshot);
        }

        return result;
    }
}