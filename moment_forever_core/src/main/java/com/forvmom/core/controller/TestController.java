package com.forvmom.core.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Development-only Kafka smoke test that publishes an arbitrary string message.
 *
 * <p>
 * Used to verify broker connectivity end to end without going through the booking
 * flow. It publishes to the literal topic {@code booking-requested}, which is
 * <em>not</em> the configured booking topic ({@code kafka.topics.booking-requested},
 * default {@code topic_booking_requested}), so it cannot interfere with real
 * booking events.
 *
 * <p>
 * <strong>⚠️ This endpoint is unauthenticated and publishes to Kafka. Remove it or
 * restrict it before deploying outside local development.</strong>
 */
@RestController
@RequestMapping("/public/kafka")
public class TestController {
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes a plain-text message to the test topic.
     *
     * @param message arbitrary payload to publish
     * @return confirmation echoing the published message
     */
    @PostMapping("/send-to-booking")
    public String send(@RequestParam String message) {
        kafkaTemplate.send("booking-requested", message);
        return "Sent: " + message;
    }
}