package com.forvmom.core;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Development-only Kafka consumer paired with
 * {@link com.forvmom.core.controller.TestController} to prove round-trip broker
 * connectivity.
 *
 * <p>
 * Bound to the literal topic {@code booking-requested}, which is distinct from the
 * configured booking topic ({@code kafka.topics.booking-requested}), so it never
 * consumes real booking events.
 *
 * <p>
 * <strong>⚠️ Diagnostic scaffolding — it prints to stdout rather than logging and
 * should be removed before production.</strong>
 */
@Component
public class TestListener {
    /**
     * Prints the received message and acknowledges it.
     *
     * @param message raw message payload
     * @param ack     manual acknowledgment handle
     */
    @KafkaListener(topics = "booking-requested")
    public void listen(String message, Acknowledgment ack) {
        System.out.println("Received: " + message);
        ack.acknowledge();
    }
}