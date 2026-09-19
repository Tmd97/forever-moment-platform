package com.forvmom.core.retries.service;

import com.forvmom.core.retries.model.BookingOutboxCompensationResult;
import com.forvmom.core.services.BookingReservationService;
import com.forvmom.data.dao.BookingOutboxDao;
import com.forvmom.data.entities.BookingOutbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingOutboxCompensationTransactionServiceTest {

    @Mock
    private BookingOutboxDao bookingOutboxDao;
    @Mock
    private BookingReservationService bookingReservationService;

    private BookingOutboxCompensationTransactionService service;

    @BeforeEach
    void setUp() {
        service = new BookingOutboxCompensationTransactionService(
                bookingOutboxDao,
                bookingReservationService);
    }

    @Test
    void marksOutboxCompensatedAfterReservationRelease() {
        BookingOutbox outboxRecord = outbox("MFB-100");
        LocalDate bookingDate = LocalDate.of(2026, 8, 24);
        when(bookingOutboxDao.findForUpdate("MFB-100"))
                .thenReturn(outboxRecord);
        when(bookingReservationService.releaseOnce(
                "MFB-100", 10L, bookingDate, 4))
                .thenReturn(true);
        when(bookingOutboxDao.markCompensated(
                org.mockito.ArgumentMatchers.eq("MFB-100"),
                org.mockito.ArgumentMatchers.eq(5),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(1);

        BookingOutboxCompensationResult result =
                service.compensate("MFB-100", 10L, bookingDate, 4, 5);

        assertTrue(result.applied());
        assertTrue(result.released());
        verify(bookingOutboxDao).markCompensated(
                org.mockito.ArgumentMatchers.eq("MFB-100"),
                org.mockito.ArgumentMatchers.eq(5),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void marksOutboxDeadInIndependentTransition() {
        BookingOutbox outboxRecord = outbox("MFB-100");
        when(bookingOutboxDao.findForUpdate("MFB-100"))
                .thenReturn(outboxRecord);
        when(bookingOutboxDao.markDead("MFB-100", 5, "inventory update failed"))
                .thenReturn(1);

        assertTrue(service.markDead("MFB-100", "inventory update failed", 5));

        verify(bookingOutboxDao).markDead(
                "MFB-100", 5, "inventory update failed");
    }

    @Test
    void activeNewOwnerPreventsStaleCompensation() {
        BookingOutbox outboxRecord = outbox("MFB-100");
        outboxRecord.setStatus(BookingOutbox.STATUS_PROCESSING);
        outboxRecord.setProcessingOwnerToken("owner-2");
        when(bookingOutboxDao.findForUpdate("MFB-100"))
                .thenReturn(outboxRecord);

        BookingOutboxCompensationResult result =
                service.compensate(
                        "MFB-100",
                        10L,
                        LocalDate.of(2026, 8, 24),
                        4,
                        5);

        assertFalse(result.applied());
        verify(bookingReservationService, never())
                .releaseOnce(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    private BookingOutbox outbox(String bookingReferenceId) {
        BookingOutbox outboxRecord = new BookingOutbox();
        outboxRecord.setBookingReferenceId(bookingReferenceId);
        outboxRecord.setStatus(BookingOutbox.STATUS_FAILED);
        outboxRecord.setRetryCount(5);
        return outboxRecord;
    }
}
