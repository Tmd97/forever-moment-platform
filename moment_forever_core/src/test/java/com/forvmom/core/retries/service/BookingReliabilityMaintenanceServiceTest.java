package com.forvmom.core.retries.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forvmom.core.event_enrichment.BookingEnrichmentTask;
import com.forvmom.core.retries.model.BookingOutboxRecoveryBatch;
import com.forvmom.data.dao.BookingOutboxDao;
import com.forvmom.data.dao.BookingRequestIdempotencyDao;
import com.forvmom.data.entities.BookingOutbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingReliabilityMaintenanceServiceTest {

    @Mock
    private BookingOutboxDao bookingOutboxDao;
    @Mock
    private BookingRequestIdempotencyDao bookingRequestIdempotencyDao;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private BookingOutboxCompensationTransactionService compensationTransactionService;
    @Mock
    private BookingOutboxRecoveryTransactionService recoveryTransactionService;
    @Mock
    private BookingEnrichmentTask bookingEnrichmentTask;
    @InjectMocks
    private BookingReliabilityMaintenanceService service;

    @Test
    void dispatchesRetriesAfterRecoveryPreparationCommits() {
        BookingOutbox record = new BookingOutbox();
        record.setBookingReferenceId("MFB-100");
        record.setRetryCount(1);

        when(recoveryTransactionService.prepareRecovery(
                any(LocalDateTime.class),
                any(LocalDateTime.class)))
                .thenReturn(new BookingOutboxRecoveryBatch(0, List.of(record)));

        service.recoverUnresolvedOutboxRecords();

        verify(bookingEnrichmentTask).enrich("MFB-100");
    }

    @Test
    void cleansCompletedIdempotencyRecordsWithDailyRetentionSweep() {
        when(bookingOutboxDao.deletePublishedOlderThan(any(LocalDateTime.class))).thenReturn(0);
        when(bookingRequestIdempotencyDao.deleteCompletedOlderThan(any(LocalDateTime.class)))
                .thenReturn(2);

        service.cleanupExpiredRecords();

        verify(bookingRequestIdempotencyDao)
                .deleteCompletedOlderThan(any(LocalDateTime.class));
    }

    @Test
    void recordsDeadStateAfterCompensationTransactionRollsBack() throws Exception {
        BookingOutbox record = new BookingOutbox();
        record.setBookingReferenceId("MFB-100");
        record.setPayload("""
                {"slotMapperId":10,"guestCount":4,"bookingDate":"2026-08-24"}
                """);
        when(objectMapper.readValue(
                org.mockito.ArgumentMatchers.eq(record.getPayload()),
                org.mockito.ArgumentMatchers.<com.fasterxml.jackson.core.type.TypeReference<
                        java.util.Map<String, Object>>>any()))
                .thenReturn(java.util.Map.of(
                        "slotMapperId", 10,
                        "guestCount", 4,
                        "bookingDate", "2026-08-24"));
        doThrow(new IllegalStateException("inventory update failed"))
                .when(compensationTransactionService)
                .compensate(
                        "MFB-100",
                        10L,
                        java.time.LocalDate.of(2026, 8, 24),
                        4,
                        5);

        service.compensatePermanentlyFailedBooking(record);

        verify(compensationTransactionService)
                .markDead("MFB-100", "inventory update failed", 5);
    }
}
