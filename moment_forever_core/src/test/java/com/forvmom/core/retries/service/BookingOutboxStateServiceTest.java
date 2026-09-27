package com.forvmom.core.retries.service;

import com.forvmom.core.retries.model.BookingOutboxProcessingClaim;
import com.forvmom.data.dao.BookingOutboxDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingOutboxStateServiceTest {

    @Mock
    private BookingOutboxDao bookingOutboxDao;

    @Test
    void successfulClaimReturnsThePersistedOwnerToken() {
        when(bookingOutboxDao.markAsProcessing(
                eq("MFB-100"),
                any(String.class),
                any(LocalDateTime.class)))
                .thenReturn(1);
        BookingOutboxStateService service =
                new BookingOutboxStateService(bookingOutboxDao);
        ArgumentCaptor<String> ownerToken = ArgumentCaptor.forClass(String.class);

        BookingOutboxProcessingClaim claim =
                service.claimForProcessing("MFB-100");

        verify(bookingOutboxDao).markAsProcessing(
                eq("MFB-100"),
                ownerToken.capture(),
                any(LocalDateTime.class));
        assertNotNull(claim);
        assertEquals(ownerToken.getValue(), claim.ownerToken());
    }

    @Test
    void losingClaimReturnsNoOwnership() {
        when(bookingOutboxDao.markAsProcessing(
                eq("MFB-100"),
                any(String.class),
                any(LocalDateTime.class)))
                .thenReturn(0);
        BookingOutboxStateService service =
                new BookingOutboxStateService(bookingOutboxDao);

        assertNull(service.claimForProcessing("MFB-100"));
    }

    @Test
    void staleOwnerCannotMarkNewAttemptFailed() {
        BookingOutboxProcessingClaim staleClaim =
                new BookingOutboxProcessingClaim("MFB-100", "owner-1");
        when(bookingOutboxDao.markFailed(
                "MFB-100",
                "owner-1",
                "broker unavailable"))
                .thenReturn(0);
        BookingOutboxStateService service =
                new BookingOutboxStateService(bookingOutboxDao);

        assertFalse(service.markFailed(staleClaim, "broker unavailable"));
    }
}
