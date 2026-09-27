package com.forvmom.core.services;

import com.forvmom.common.dto.request.BookingRequestDto;
import com.forvmom.core.idempotency.request.model.BookingRequestIdempotencyClaim;
import com.forvmom.core.idempotency.request.model.BookingInitiationResult;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookingCreationRetryServiceTest {

    @Mock
    private BookingCreationTransactionService bookingCreationTransactionService;

    private BookingCreationRetryService service;
    private BookingRequestDto request;
    private BookingRequestIdempotencyClaim claim;
    private BookingInitiationResult result;

    @BeforeEach
    void setUp() {
        service = new BookingCreationRetryService(bookingCreationTransactionService);
        request = new BookingRequestDto();
        request.setTimeSlotMapperId(100L);
        request.setBookingDate(LocalDate.of(2026, 8, 20));
        request.setGuestCount(4);
        claim = new BookingRequestIdempotencyClaim(
                "CREATE_BOOKING", "7", "booking-key", "fingerprint", "owner-1");
        result = new BookingInitiationResult("MFB-100", 202, "{}", false);
    }

    @Test
    void retriesEntireTransactionAfterOptimisticLockConflict() {
        doThrow(new ObjectOptimisticLockingFailureException("SlotInventory", 1L))
                .doReturn(result)
                .when(bookingCreationTransactionService)
                .reserveCapacityAndCreateOutbox(claim, request, 7L);

        service.reserveCapacityAndCreateOutbox(claim, request, 7L);

        verify(bookingCreationTransactionService, times(2))
                .reserveCapacityAndCreateOutbox(claim, request, 7L);
    }

    @Test
    void retriesEntireTransactionAfterConcurrentFirstInsert() {
        ConstraintViolationException constraintViolation = new ConstraintViolationException(
                "duplicate inventory",
                new SQLException("duplicate key"),
                "uk_slot_inventory_mapper_date");
        DataIntegrityViolationException conflict =
                new DataIntegrityViolationException("duplicate inventory", constraintViolation);

        doThrow(conflict)
                .doReturn(result)
                .when(bookingCreationTransactionService)
                .reserveCapacityAndCreateOutbox(claim, request, 7L);

        service.reserveCapacityAndCreateOutbox(claim, request, 7L);

        verify(bookingCreationTransactionService, times(2))
                .reserveCapacityAndCreateOutbox(claim, request, 7L);
    }

    @Test
    void doesNotRetryUnrelatedIntegrityViolation() {
        DataIntegrityViolationException exception =
                new DataIntegrityViolationException("outbox constraint failed");
        doThrow(exception)
                .when(bookingCreationTransactionService)
                .reserveCapacityAndCreateOutbox(claim, request, 7L);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> service.reserveCapacityAndCreateOutbox(claim, request, 7L));

        verify(bookingCreationTransactionService)
                .reserveCapacityAndCreateOutbox(claim, request, 7L);
    }

    @Test
    void stopsAfterThreeOptimisticLockConflicts() {
        doThrow(new ObjectOptimisticLockingFailureException("SlotInventory", 1L))
                .when(bookingCreationTransactionService)
                .reserveCapacityAndCreateOutbox(claim, request, 7L);

        assertThrows(
                IllegalStateException.class,
                () -> service.reserveCapacityAndCreateOutbox(claim, request, 7L));

        verify(bookingCreationTransactionService, times(3))
                .reserveCapacityAndCreateOutbox(claim, request, 7L);
    }
}
