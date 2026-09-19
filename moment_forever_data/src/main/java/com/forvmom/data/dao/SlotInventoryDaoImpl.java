package com.forvmom.data.dao;

import com.forvmom.data.entities.SlotInventory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
@Transactional
public class SlotInventoryDaoImpl extends GenericDaoImpl<SlotInventory, Long>
        implements SlotInventoryDao {

    public SlotInventoryDaoImpl() {
        super(SlotInventory.class);
    }

    @Override
    public SlotInventory findBySlotMapperIdAndBookingDate(Long slotMapperId, LocalDate bookingDate) {
        List<SlotInventory> results = em.createQuery("""
                        SELECT inventory
                        FROM SlotInventory inventory
                        WHERE inventory.slotMapper.id = :slotMapperId
                          AND inventory.bookingDate = :bookingDate
                        """, SlotInventory.class)
                .setParameter("slotMapperId", slotMapperId)
                .setParameter("bookingDate", bookingDate)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
}
