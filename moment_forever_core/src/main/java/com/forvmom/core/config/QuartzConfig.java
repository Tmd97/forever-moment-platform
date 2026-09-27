package com.forvmom.core.config;

import com.forvmom.core.scheduler.OutboxCleanupJob;
import com.forvmom.core.scheduler.OutboxRetriesJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Schedules outbox retry every minute and retention cleanup every day.
 *
 * <p>The JDBC job store coordinates multiple application instances. After
 * downtime, each trigger runs once immediately instead of replaying every missed run.
 *
 * @see com.forvmom.core.retries.service.BookingReliabilityMaintenanceService
 */
@Configuration
public class QuartzConfig {

    /** Retention sweep cadence: published outbox rows are pruned once a day. */
    private static final int CLEANUP_INTERVAL_HOURS = 24;

    /**
     * Retry sweep cadence. Kept short so a transient Kafka outage delays a booking
     * by roughly a minute rather than by the compensation window.
     */
    private static final int RETRY_INTERVAL_MINUTES = 1;

    /**
     * Durable job definition for the daily published-record cleanup.
     *
     * @return job detail for {@link OutboxCleanupJob}
     */
    @Bean
    public JobDetail outboxCleanupJobDetail() {
        return JobBuilder.newJob(OutboxCleanupJob.class)
                .withIdentity("outboxCleanupJob")
                .storeDurably()
                .build();
    }

    /**
     * Trigger firing {@link OutboxCleanupJob} every {@value #CLEANUP_INTERVAL_HOURS}
     * hours.
     *
     * @return the cleanup trigger
     */
    @Bean
    public Trigger outboxCleanupTrigger() {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInHours(CLEANUP_INTERVAL_HOURS)
                .repeatForever()
                .withMisfireHandlingInstructionFireNow();

        return TriggerBuilder.newTrigger()
                .forJob(outboxCleanupJobDetail())
                .withIdentity("outboxCleanupTrigger")
                .withDescription("Runs every 24 hours to clean up old published records")
                .withSchedule(scheduleBuilder)
                .build();
    }

    /**
     * Durable job definition for the outbox retry/compensation poller.
     *
     * @return job detail for {@link OutboxRetriesJob}
     */
    @Bean
    public JobDetail outboxRetryJobDetail() {
        return JobBuilder.newJob(OutboxRetriesJob.class)
                .withIdentity("outboxRetryJob")
                .storeDurably()
                .build();
    }

    /**
     * Trigger firing {@link OutboxRetriesJob} every
     * {@value #RETRY_INTERVAL_MINUTES} minute(s).
     *
     * @return the retry trigger
     */
    @Bean
    public Trigger outboxRetryTrigger() {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInMinutes(RETRY_INTERVAL_MINUTES)
                .repeatForever()
                .withMisfireHandlingInstructionFireNow();

        return TriggerBuilder.newTrigger()
                .forJob(outboxRetryJobDetail())
                .withIdentity("outboxRetryTrigger")
                .withDescription("Recovers timed-out and failed booking outbox records")
                .withSchedule(scheduleBuilder)
                .build();
    }
}