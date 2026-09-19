package com.forvmom.data.dao;

import com.forvmom.data.entities.BookingReservation;

import java.time.LocalDateTime;

public interface BookingReservationDao extends GenericDao<BookingReservation, String> {

    int markReleasedIfReserved(String bookingReferenceId, LocalDateTime releasedAt);
}
