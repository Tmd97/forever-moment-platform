package com.forvmom.core.services;

import com.forvmom.data.dao.BookingReservationDao;
import com.forvmom.data.entities.BookingReservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingReservationServiceTest {

    private static final String BOOKING_ID = "MFB-100";
    private static final Long SLOT_MAPPER_ID = 100L;
    private static final LocalDate BOOKING_DATE = LocalDate.of(2026, 8, 20);
    private static final int GUEST_COUNT = 4;

    @Mock
    private BookingReservationDao bookingReservationDao;
    @Mock
    private SlotInventoryService slotInventoryService;

    private BookingReservationService service;

    @BeforeEach
    void setUp() {
        service = new BookingReservationService(bookingReservationDao, slotInventoryService);
    }

    @Test
    void firstFailureReleasesReservedCapacity() {
        BookingReservation reservation = reservation(BookingReservation.STATUS_RELEASED);
        when(bookingReservationDao.markReleasedIfReserved(
                org.mockito.ArgumentMatchers.eq(BOOKING_ID),
                any(LocalDateTime.class)))
                .thenReturn(1);
        when(bookingReservationDao.findById(BOOKING_ID)).thenReturn(reservation);

        boolean released = service.releaseOnce(
                BOOKING_ID,
                SLOT_MAPPER_ID,
                BOOKING_DATE,
                GUEST_COUNT);

        assertTrue(released);
        verify(slotInventoryService).releaseCapacity(
                SLOT_MAPPER_ID,
                BOOKING_DATE,
                GUEST_COUNT);
    }

    @Test
    void duplicateFailureDoesNotReleaseCapacityAgain() {
        BookingReservation reservation = reservation(BookingReservation.STATUS_RELEASED);
        when(bookingReservationDao.markReleasedIfReserved(
                org.mockito.ArgumentMatchers.eq(BOOKING_ID),
                any(LocalDateTime.class)))
                .thenReturn(0);
        when(bookingReservationDao.findById(BOOKING_ID)).thenReturn(reservation);

        boolean released = service.releaseOnce(
                BOOKING_ID,
                SLOT_MAPPER_ID,
                BOOKING_DATE,
                GUEST_COUNT);

        assertFalse(released);
        verify(slotInventoryService, never()).releaseCapacity(
                SLOT_MAPPER_ID,
                BOOKING_DATE,
                GUEST_COUNT);
    }

    @Test
    void rejectsFailureEventThatDoesNotMatchReservation() {
        BookingReservation reservation = reservation(BookingReservation.STATUS_RELEASED);
        when(bookingReservationDao.markReleasedIfReserved(
                org.mockito.ArgumentMatchers.eq(BOOKING_ID),
                any(LocalDateTime.class)))
                .thenReturn(1);
        when(bookingReservationDao.findById(BOOKING_ID)).thenReturn(reservation);

        assertThrows(
                IllegalStateException.class,
                () -> service.releaseOnce(
                        BOOKING_ID,
                        SLOT_MAPPER_ID,
                        BOOKING_DATE.plusDays(1),
                        GUEST_COUNT));

        verify(slotInventoryService, never()).releaseCapacity(
                SLOT_MAPPER_ID,
                BOOKING_DATE,
                GUEST_COUNT);
    }

    @Test
    void missingReservationFailsInsteadOfReleasingFromEventData() {
        when(bookingReservationDao.markReleasedIfReserved(
                org.mockito.ArgumentMatchers.eq(BOOKING_ID),
                any(LocalDateTime.class)))
                .thenReturn(0);
        when(bookingReservationDao.findById(BOOKING_ID)).thenReturn(null);

        assertThrows(
                IllegalStateException.class,
                () -> service.releaseOnce(
                        BOOKING_ID,
                        SLOT_MAPPER_ID,
                        BOOKING_DATE,
                        GUEST_COUNT));

        verify(slotInventoryService, never()).releaseCapacity(
                SLOT_MAPPER_ID,
                BOOKING_DATE,
                GUEST_COUNT);
    }

    private BookingReservation reservation(String status) {
        BookingReservation reservation = new BookingReservation();
        reservation.setBookingReferenceId(BOOKING_ID);
        reservation.setSlotMapperId(SLOT_MAPPER_ID);
        reservation.setBookingDate(BOOKING_DATE);
        reservation.setGuestCount(GUEST_COUNT);
        reservation.setStatus(status);
        return reservation;
    }
}
