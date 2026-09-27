package com.forvmom.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configures the dedicated thread pool for the async booking enrichment task
 * and enables Spring's {@code @Async} and {@code @Scheduled} processing.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class BookingThreadPoolConfig {

    /**
     * Creates the bounded executor used by fast-path and scheduled outbox
     * enrichment. Its maximum concurrency stays below the database connection
     * pool size because each publication holds a row lock while awaiting Kafka.
     */
    @Bean(name = "bookingTaskExecutor")
    public Executor bookingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("booking-enrich-");
        executor.setKeepAliveSeconds(60);
        executor.initialize();
        return executor;
    }
}
