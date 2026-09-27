package com.forvmom.core.retries.service;

import com.forvmom.common.dto.events.BookingRequestEvent;
import com.forvmom.core.producer.BookingEventProducer;
import com.forvmom.core.retries.model.BookingOutboxProcessingClaim;
import com.forvmom.data.dao.BookingOutboxDao;
import com.forvmom.data.entities.BookingOutbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingOutboxPublicationTransactionServiceTest {

    private static final BookingOutboxProcessingClaim CLAIM =
            new BookingOutboxProcessingClaim("MFB-100", "owner-1");

    @Mock
    private BookingOutboxDao bookingOutboxDao;
    @Mock
    private BookingEventProducer bookingEventProducer;

    private BookingOutboxPublicationTransactionService service;
    private BookingRequestEvent event;

    @BeforeEach
    void setUp() {
        service = new BookingOutboxPublicationTransactionService(
                bookingOutboxDao,
                bookingEventProducer);
        event = new BookingRequestEvent();
        event.setBookingId("MFB-100");
    }

    @Test
    void locksAndVerifiesOwnershipBeforePublishing() {
        BookingOutbox outboxRecord = ownedOutbox("owner-1");
        when(bookingOutboxDao.findForUpdate("MFB-100")).thenReturn(outboxRecord);
        when(bookingEventProducer.sendBookingRequested(event))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(bookingOutboxDao.markPublished(
                org.mockito.ArgumentMatchers.eq("MFB-100"),
                org.mockito.ArgumentMatchers.eq("owner-1"),
                any(LocalDateTime.class)))
                .thenReturn(1);

        assertTrue(service.publish(CLAIM, event));

        InOrder order = inOrder(bookingOutboxDao, bookingEventProducer);
        order.verify(bookingOutboxDao).findForUpdate("MFB-100");
        order.verify(bookingEventProducer).sendBookingRequested(event);
        order.verify(bookingOutboxDao).markPublished(
                org.mockito.ArgumentMatchers.eq("MFB-100"),
                org.mockito.ArgumentMatchers.eq("owner-1"),
                any(LocalDateTime.class));
    }

    @Test
    void staleTokenCannotPublish() {
        when(bookingOutboxDao.findForUpdate("MFB-100"))
                .thenReturn(ownedOutbox("owner-2"));

        assertFalse(service.publish(CLAIM, event));

        verify(bookingEventProducer, never()).sendBookingRequested(any());
        verify(bookingOutboxDao, never()).markPublished(any(), any(), any());
    }

    @Test
    void brokerFailureDoesNotMarkPublished() {
        when(bookingOutboxDao.findForUpdate("MFB-100"))
                .thenReturn(ownedOutbox("owner-1"));
        CompletableFuture<org.springframework.kafka.support.SendResult<String, Object>> failed =
                new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("broker unavailable"));
        when(bookingEventProducer.sendBookingRequested(event)).thenReturn(failed);

        assertThrows(CompletionException.class, () -> service.publish(CLAIM, event));

        verify(bookingOutboxDao, never()).markPublished(any(), any(), any());
    }

    private BookingOutbox ownedOutbox(String ownerToken) {
        BookingOutbox outboxRecord = new BookingOutbox();
        outboxRecord.setBookingReferenceId("MFB-100");
        outboxRecord.setStatus(BookingOutbox.STATUS_PROCESSING);
        outboxRecord.setProcessingOwnerToken(ownerToken);
        return outboxRecord;
    }
}
