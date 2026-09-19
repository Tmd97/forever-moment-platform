package com.forvmom.data.dao;

import com.forvmom.data.entities.SlotInventory;

import java.time.LocalDate;

public interface SlotInventoryDao extends GenericDao<SlotInventory, Long> {

    SlotInventory findBySlotMapperIdAndBookingDate(Long slotMapperId, LocalDate bookingDate);
}
