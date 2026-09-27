package com.forvmom.core.services;

import com.forvmom.data.dao.BookingReservationDao;
import com.forvmom.data.entities.BookingReservation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class BookingReservationService {

    private final BookingReservationDao bookingReservationDao;
    private final SlotInventoryService slotInventoryService;

    public BookingReservationService(
            BookingReservationDao bookingReservationDao,
            SlotInventoryService slotInventoryService
    ) {
        this.bookingReservationDao = bookingReservationDao;
        this.slotInventoryService = slotInventoryService;
    }

    @Transactional
    public boolean releaseOnce(String bookingReferenceId) {
        return releaseOnce(bookingReferenceId, null, null, null);
    }

    @Transactional
    public boolean releaseOnce(
            String bookingReferenceId,
            Long expectedSlotMapperId,
            LocalDate expectedBookingDate,
            Integer expectedGuestCount
    ) {
        validateBookingReference(bookingReferenceId);

        int updated = bookingReservationDao.markReleasedIfReserved(
                bookingReferenceId,
                LocalDateTime.now());
        BookingReservation reservation = bookingReservationDao.findById(bookingReferenceId);

        if (reservation == null) {
            throw new IllegalStateException(
                    "Booking reservation not found: " + bookingReferenceId);
        }

        validateExpectedDetails(
                reservation,
                expectedSlotMapperId,
                expectedBookingDate,
                expectedGuestCount);

        if (updated == 0) {
            if (BookingReservation.STATUS_RELEASED.equals(reservation.getStatus())) {
                return false;
            }
            throw new IllegalStateException(
                    "Booking reservation could not be released from status "
                            + reservation.getStatus() + ": " + bookingReferenceId);
        }

        slotInventoryService.releaseCapacity(
                reservation.getSlotMapperId(),
                reservation.getBookingDate(),
                reservation.getGuestCount());
        return true;
    }

    private void validateBookingReference(String bookingReferenceId) {
        if (bookingReferenceId == null || bookingReferenceId.isBlank()) {
            throw new IllegalArgumentException("bookingReferenceId is required");
        }
    }

    private void validateExpectedDetails(
            BookingReservation reservation,
            Long expectedSlotMapperId,
            LocalDate expectedBookingDate,
            Integer expectedGuestCount
    ) {
        if (expectedSlotMapperId == null
                && expectedBookingDate == null
                && expectedGuestCount == null) {
            return;
        }

        if (!Objects.equals(reservation.getSlotMapperId(), expectedSlotMapperId)
                || !Objects.equals(reservation.getBookingDate(), expectedBookingDate)
                || !Objects.equals(reservation.getGuestCount(), expectedGuestCount)) {
            throw new IllegalStateException(
                    "Booking failure details do not match reservation: "
                            + reservation.getBookingReferenceId());
        }
    }
}
