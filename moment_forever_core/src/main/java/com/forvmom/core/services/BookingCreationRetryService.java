package com.forvmom.core.services;

import com.forvmom.common.dto.request.BookingRequestDto;
import com.forvmom.core.idempotency.request.model.BookingRequestIdempotencyClaim;
import com.forvmom.core.idempotency.request.model.BookingInitiationResult;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/**
 * Retries short database races while creating the booking transaction.
 *
 * <p>This is separate from outbox retries. It retries only inventory lock or
 * inventory-row creation conflicts.
 */
@Service
public class BookingCreationRetryService {

    private static final Logger logger =
            LoggerFactory.getLogger(BookingCreationRetryService.class);
    private static final String INVENTORY_UNIQUE_CONSTRAINT = "uk_slot_inventory_mapper_date";
    /** Maximum immediate attempts for a database concurrency conflict. */
    private static final int MAX_ATTEMPTS = 3;

    private final BookingCreationTransactionService bookingCreationTransactionService;

    public BookingCreationRetryService(
            BookingCreationTransactionService bookingCreationTransactionService
    ) {
        this.bookingCreationTransactionService = bookingCreationTransactionService;
    }

    /**
     * Runs the booking transaction up to three times for known concurrency conflicts.
     * Other failures are returned immediately.
     */
    public BookingInitiationResult reserveCapacityAndCreateOutbox(
            BookingRequestIdempotencyClaim claim,
            BookingRequestDto bookingRequest,
            Long userId
    ) {
        RuntimeException lastConflict = null;

        // Every attempt starts a new transaction in BookingCreationTransactionService.
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                //TODO: in this method below, we are also storing the idempotency request record, so name change of this method required
                return bookingCreationTransactionService.reserveCapacityAndCreateOutbox(
                        claim, bookingRequest, userId);
            } catch (ObjectOptimisticLockingFailureException exception) {
                lastConflict = exception;
                logger.debug(
                        "Retrying booking reservation after optimistic lock conflict: callerId={}, attempt={}",
                        claim.callerId(),
                        attempt);
            } catch (DataIntegrityViolationException exception) {
                if (!isInventoryCreationConflict(exception)) {
                    throw exception;
                }
                lastConflict = exception;
                logger.debug(
                        "Retrying booking reservation after concurrent inventory creation: callerId={}, attempt={}",
                        claim.callerId(),
                        attempt);
            }
        }

        throw new IllegalStateException(
                "Could not reserve inventory after " + MAX_ATTEMPTS
                        + " attempts for callerId=" + claim.callerId(),
                lastConflict);
    }

    /**
     * Checks whether the failure is the expected race to create one inventory row.
     */
    private boolean isInventoryCreationConflict(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation) {
                return INVENTORY_UNIQUE_CONSTRAINT.equalsIgnoreCase(
                        constraintViolation.getConstraintName());
            }
            cause = cause.getCause();
        }
        return false;
    }
}