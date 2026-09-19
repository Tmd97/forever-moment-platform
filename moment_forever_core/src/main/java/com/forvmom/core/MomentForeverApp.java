package com.forvmom.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Entry point of the <strong>Platform</strong> service — the core microservice of the
 * Moment Forever event-driven platform.
 *
 * <p>
 * This service owns the catalog (experiences, locations, time slots, add-ons),
 * media, identity, and slot inventory. It <em>initiates</em> the booking saga by
 * reserving capacity and publishing {@code booking-requested}, then reacts to
 * {@code booking-failed} by compensating that reservation. Booking records and
 * payments are owned by the Booking and Payment microservices respectively.
 *
 * <p>
 * Component scanning is widened to {@code com.forvmom} because the runnable
 * application lives in {@code moment_forever_core} while beans it depends on are
 * contributed by the sibling library modules ({@code commons}, {@code data},
 * {@code security}, {@code object_store}). Entity scanning is declared explicitly
 * for the same reason — JPA entities are split across the {@code data} and
 * {@code security} modules and would not be discovered from this package alone.
 *
 * @see com.forvmom.core.services.BookingOrchestrationService
 * @see com.forvmom.core.event_enrichment.BookingEnrichmentTask
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {
        "com.forvmom"
})
@EntityScan(basePackages = {
        "com.forvmom.data.entities",
        "com.forvmom.security.entities" // If security has entities
})

public class MomentForeverApp {

    /**
     * Boots the Platform service.
     *
     * @param args standard Spring Boot command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(MomentForeverApp.class, args);
    }
}