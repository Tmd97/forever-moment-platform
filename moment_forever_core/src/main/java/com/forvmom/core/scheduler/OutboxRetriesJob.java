package com.forvmom.core.scheduler;
import com.forvmom.core.retries.service.BookingReliabilityMaintenanceService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Runs the outbox recovery sweep every minute.
 *
 * <p>The job contains no retry logic itself. It only calls the maintenance
 * service, which resets timed-out rows, retries failures, and compensates rows
 * that reached the maximum retry count.
 */
@Component
public class OutboxRetriesJob implements Job {

    @Autowired
    private BookingReliabilityMaintenanceService reliabilityMaintenanceService;

    /**
     * Starts one recovery sweep.
     *
     * @param jobExecutionContext Quartz execution context (unused)
     * @throws JobExecutionException required by the Quartz job contract
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        reliabilityMaintenanceService.recoverUnresolvedOutboxRecords();
    }
}
