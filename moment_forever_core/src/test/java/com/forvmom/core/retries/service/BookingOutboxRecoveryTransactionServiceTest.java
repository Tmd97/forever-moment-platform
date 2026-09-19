package com.forvmom.core.retries.service;

import com.forvmom.core.retries.model.BookingOutboxRecoveryBatch;
import com.forvmom.data.dao.BookingOutboxDao;
import com.forvmom.data.entities.BookingOutbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingOutboxRecoveryTransactionServiceTest {

    @Mock
    private BookingOutboxDao bookingOutboxDao;

    @Test
    void resetsStaleOwnershipBeforeLoadingRecoveryCandidates() {
        LocalDateTime stuckCutoff = LocalDateTime.of(2026, 8, 23, 19, 0);
        LocalDateTime unresolvedCutoff = LocalDateTime.of(2026, 8, 23, 19, 3);
        BookingOutbox outboxRecord = new BookingOutbox();
        when(bookingOutboxDao.resetStuckProcessing(stuckCutoff)).thenReturn(1);
        when(bookingOutboxDao.findUnresolved(unresolvedCutoff))
                .thenReturn(List.of(outboxRecord));
        BookingOutboxRecoveryTransactionService service =
                new BookingOutboxRecoveryTransactionService(bookingOutboxDao);

        BookingOutboxRecoveryBatch batch =
                service.prepareRecovery(stuckCutoff, unresolvedCutoff);

        assertEquals(1, batch.resetCount());
        assertEquals(List.of(outboxRecord), batch.unresolvedRecords());
        InOrder order = inOrder(bookingOutboxDao);
        order.verify(bookingOutboxDao).resetStuckProcessing(stuckCutoff);
        order.verify(bookingOutboxDao).findUnresolved(unresolvedCutoff);
    }
}
