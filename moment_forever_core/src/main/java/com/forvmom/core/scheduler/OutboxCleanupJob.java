package com.forvmom.core.scheduler;
import com.forvmom.core.retries.service.BookingReliabilityMaintenanceService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Runs the retention cleanup once per day.
 *
 * <p>The maintenance service deletes old published outbox rows and completed
 * HTTP idempotency replay rows.
 */
@Component
public class OutboxCleanupJob implements Job {

    @Autowired
    private BookingReliabilityMaintenanceService reliabilityMaintenanceService;

    /**
     * Starts one cleanup sweep.
     *
     * @param jobExecutionContext Quartz execution context (unused)
     * @throws JobExecutionException required by the Quartz job contract
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        reliabilityMaintenanceService.cleanupExpiredRecords();
    }
}
