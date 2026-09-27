package com.forvmom.data.dao;

import com.forvmom.data.entities.BookingReservation;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
@Transactional
public class BookingReservationDaoImpl
        extends GenericDaoImpl<BookingReservation, String>
        implements BookingReservationDao {

    public BookingReservationDaoImpl() {
        super(BookingReservation.class);
    }

    @Override
    public int markReleasedIfReserved(String bookingReferenceId, LocalDateTime releasedAt) {
        return em.createQuery("""
                        UPDATE BookingReservation reservation
                        SET reservation.status = :released,
                            reservation.releasedAt = :releasedAt,
                            reservation.version = reservation.version + 1
                        WHERE reservation.bookingReferenceId = :bookingReferenceId
                          AND reservation.status = :reserved
                        """)
                .setParameter("released", BookingReservation.STATUS_RELEASED)
                .setParameter("releasedAt", releasedAt)
                .setParameter("bookingReferenceId", bookingReferenceId)
                .setParameter("reserved", BookingReservation.STATUS_RESERVED)
                .executeUpdate();
    }
}
