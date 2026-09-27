package com.forvmom.core.services;

import com.forvmom.data.dao.ExperienceTimeSlotMapperDao;
import com.forvmom.data.dao.SlotInventoryDao;
import com.forvmom.data.entities.ExperienceTimeSlotMapper;
import com.forvmom.data.entities.SlotInventory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class SlotInventoryService {

    private final ExperienceTimeSlotMapperDao slotMapperDao;
    private final SlotInventoryDao slotInventoryDao;

    public SlotInventoryService(
            ExperienceTimeSlotMapperDao slotMapperDao,
            SlotInventoryDao slotInventoryDao
    ) {
        this.slotMapperDao = slotMapperDao;
        this.slotInventoryDao = slotInventoryDao;
    }

    // Under the hood it use version as we added in entity, so if two request try to update the same row, one will fail and we can retry it.
    @Transactional(propagation = Propagation.MANDATORY)
    public void reserveCapacity(Long slotMapperId, LocalDate bookingDate, int guestCount) {
        validateArguments(slotMapperId, bookingDate, guestCount);

        ExperienceTimeSlotMapper slotMapper = slotMapperDao.findById(slotMapperId);
        validateSlotMapper(slotMapper, bookingDate);

        SlotInventory inventory = slotInventoryDao.findBySlotMapperIdAndBookingDate(
                slotMapperId,
                bookingDate);

        int currentlyBooked = inventory == null ? 0 : inventory.getBookedCount();
        ensureCapacity(slotMapper.getMaxCapacity(), currentlyBooked, guestCount);

        if (inventory == null) {
            inventory = new SlotInventory();
            inventory.setSlotMapper(slotMapper);
            inventory.setBookingDate(bookingDate);
            inventory.setBookedCount(guestCount);
            slotInventoryDao.save(inventory);
            return;
        }

        inventory.setBookedCount(currentlyBooked + guestCount);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseCapacity(Long slotMapperId, LocalDate bookingDate, int guestCount) {
        validateArguments(slotMapperId, bookingDate, guestCount);

        SlotInventory inventory = slotInventoryDao.findBySlotMapperIdAndBookingDate(
                slotMapperId,
                bookingDate);
        if (inventory == null || inventory.getBookedCount() < guestCount) {
            throw new IllegalStateException(
                    "No matching inventory reservation for slotMapperId=" + slotMapperId
                            + ", bookingDate=" + bookingDate
                            + ", guestCount=" + guestCount);
        }

        inventory.setBookedCount(inventory.getBookedCount() - guestCount);
    }

    private void validateArguments(Long slotMapperId, LocalDate bookingDate, int guestCount) {
        if (slotMapperId == null || bookingDate == null) {
            throw new IllegalArgumentException("slotMapperId and bookingDate are required");
        }
        if (guestCount <= 0) {
            throw new IllegalArgumentException("guestCount must be greater than zero");
        }
    }

    private void validateSlotMapper(ExperienceTimeSlotMapper slotMapper, LocalDate bookingDate) {
        if (slotMapper == null
                || !Boolean.TRUE.equals(slotMapper.getIsActive())
                || !slotMapper.isValidOnDate(bookingDate)) {
            throw new IllegalStateException(
                    "Time slot is missing, inactive, or invalid on date " + bookingDate);
        }
    }

    private void ensureCapacity(Integer maxCapacity, int currentlyBooked, int guestCount) {
        if (maxCapacity != null && currentlyBooked + guestCount > maxCapacity) {
            throw new IllegalStateException(
                    "Time slot does not have enough capacity. Requested=" + guestCount
                            + ", booked=" + currentlyBooked
                            + ", maxCapacity=" + maxCapacity);
        }
    }
}
