package com.forvmom.core.services;

import com.forvmom.data.dao.ExperienceTimeSlotMapperDao;
import com.forvmom.data.dao.SlotInventoryDao;
import com.forvmom.data.entities.ExperienceTimeSlotMapper;
import com.forvmom.data.entities.SlotInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlotInventoryServiceTest {

    private static final LocalDate BOOKING_DATE = LocalDate.of(2026, 8, 20);

    @Mock
    private ExperienceTimeSlotMapperDao slotMapperDao;

    @Mock
    private SlotInventoryDao slotInventoryDao;

    private SlotInventoryService service;
    private ExperienceTimeSlotMapper slotMapper;

    @BeforeEach
    void setUp() {
        service = new SlotInventoryService(slotMapperDao, slotInventoryDao);
        slotMapper = new ExperienceTimeSlotMapper();
        slotMapper.setId(100L);
        slotMapper.setIsActive(true);
        slotMapper.setMaxCapacity(20);
        slotMapper.setValidFrom(BOOKING_DATE.minusDays(1));
        slotMapper.setValidTo(BOOKING_DATE.plusDays(1));
        when(slotMapperDao.findById(100L)).thenReturn(slotMapper);
    }

    @Test
    void createsFirstInventoryRowWithRequestedGuests() {
        when(slotInventoryDao.findBySlotMapperIdAndBookingDate(100L, BOOKING_DATE))
                .thenReturn(null);

        service.reserveCapacity(100L, BOOKING_DATE, 4);

        ArgumentCaptor<SlotInventory> inventoryCaptor = ArgumentCaptor.forClass(SlotInventory.class);
        verify(slotInventoryDao).save(inventoryCaptor.capture());
        assertEquals(BOOKING_DATE, inventoryCaptor.getValue().getBookingDate());
        assertEquals(4, inventoryCaptor.getValue().getBookedCount());
        assertEquals(slotMapper, inventoryCaptor.getValue().getSlotMapper());
    }

    @Test
    void incrementsExistingInventoryForTheSameDate() {
        SlotInventory inventory = inventoryWithBookedCount(12);
        when(slotInventoryDao.findBySlotMapperIdAndBookingDate(100L, BOOKING_DATE))
                .thenReturn(inventory);

        service.reserveCapacity(100L, BOOKING_DATE, 4);

        assertEquals(16, inventory.getBookedCount());
    }

    @Test
    void rejectsReservationThatExceedsCapacity() {
        SlotInventory inventory = inventoryWithBookedCount(18);
        when(slotInventoryDao.findBySlotMapperIdAndBookingDate(100L, BOOKING_DATE))
                .thenReturn(inventory);

        assertThrows(
                IllegalStateException.class,
                () -> service.reserveCapacity(100L, BOOKING_DATE, 4));

        assertEquals(18, inventory.getBookedCount());
    }

    private SlotInventory inventoryWithBookedCount(int bookedCount) {
        SlotInventory inventory = new SlotInventory();
        inventory.setSlotMapper(slotMapper);
        inventory.setBookingDate(BOOKING_DATE);
        inventory.setBookedCount(bookedCount);
        return inventory;
    }
}
