package com.forvmom.core.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaConfigTest {

    @Test
    void wrapsKafkaDeserializersForPoisonRecordRecovery() {
        KafkaConfig config = new KafkaConfig();
        Map<String, Object> properties = config.consumerProperties();

        assertEquals(
                ErrorHandlingDeserializer.class,
                properties.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
        assertEquals(
                ErrorHandlingDeserializer.class,
                properties.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));
        assertEquals(
                JsonDeserializer.class,
                properties.get(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS));
    }

    @Test
    void declaresTheConfiguredDeadLetterTopic() {
        KafkaConfig config = new KafkaConfig();

        assertEquals(KafkaConfig.DEAD_LETTER_TOPIC, config.deadLetterTopic().name());
    }

    @Test
    void routesExhaustedRecordsToTheDeclaredDeadLetterTopic() {
        ConsumerRecord<String, Object> record =
                new ConsumerRecord<>("topic_booking_failed", 3, 10L, "key", "value");

        assertEquals(
                KafkaConfig.DEAD_LETTER_TOPIC,
                KafkaConfig.deadLetterDestination(record, new IllegalStateException()).topic());
        assertEquals(
                -1,
                KafkaConfig.deadLetterDestination(record, new IllegalStateException()).partition());
    }
}
