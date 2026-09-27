package com.forvmom.core.retries.service;

import com.forvmom.core.retries.model.BookingOutboxRecoveryBatch;
import com.forvmom.data.dao.BookingOutboxDao;
import com.forvmom.data.entities.BookingOutbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Prepares one Quartz retry sweep inside a short database transaction.
 *
 * <p>It commits before retries or compensation start, so those operations do
 * not wait on locks held by this sweep.
 */
@Service
public class BookingOutboxRecoveryTransactionService {

    private static final Logger logger =
            LoggerFactory.getLogger(BookingOutboxRecoveryTransactionService.class);

    private final BookingOutboxDao bookingOutboxDao;

    public BookingOutboxRecoveryTransactionService(BookingOutboxDao bookingOutboxDao) {
        this.bookingOutboxDao = bookingOutboxDao;
    }

    /**
     * Resets timed-out workers and returns rows that still need work.
     */
    @Transactional
    public BookingOutboxRecoveryBatch prepareRecovery(
            LocalDateTime stuckCutoff,
            LocalDateTime unresolvedCutoff
    ) {
        // Reset first so timed-out rows can be included in this same sweep.
        int resetCount = bookingOutboxDao.resetStuckProcessing(stuckCutoff);
        List<BookingOutbox> unresolvedRecords =
                bookingOutboxDao.findUnresolved(unresolvedCutoff);
        logger.debug(
                "Prepared outbox recovery batch: resetCount={}, unresolvedCount={}",
                resetCount,
                unresolvedRecords.size());
        return new BookingOutboxRecoveryBatch(resetCount, unresolvedRecords);
    }
}
