package com.forvmom.core.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka wiring for the Platform service: producer, consumer, listener container
 * and topic definitions.
 *
 * <p>
 * <strong>Delivery guarantees configured here</strong>
 * <ul>
 * <li><em>Producer</em> — {@code acks=all} plus 3 retries, so a publish is only
 * considered successful once all in-sync replicas have the record. This matters
 * because {@link com.forvmom.core.event_enrichment.BookingEnrichmentTask} flips the outbox
 * row to {@code PUBLISHED} on success; a weaker ack setting would let the outbox
 * claim durability the broker never provided.</li>
 * <li><em>Consumer</em> — auto-commit is disabled and the container uses
 * {@link ContainerProperties.AckMode#MANUAL_IMMEDIATE}. Handlers acknowledge only
 * after their database work succeeds, so a crash mid-handler results in
 * redelivery rather than a silently skipped event.</li>
 * <li><em>Error handling</em> — 3 redeliveries with a 1 second fixed backoff,
 * then the record is routed to the {@code core-dlt} dead-letter topic instead of
 * blocking the partition forever.</li>
 * </ul>
 *
 * <p>
 * Deserialization is restricted to {@code com.forvmom.common.dto.events} via
 * {@link JsonDeserializer#TRUSTED_PACKAGES} — a deliberate guard against
 * deserializing arbitrary types named in an inbound message header.
 *
 * <p>
 * Topic names are injected from configuration rather than hardcoded so that
 * environments can namespace them. Topics are declared with a single partition
 * and replica, which is a development-scale setting: production sizing should
 * raise both, and partition count must stay compatible with the per-booking
 * ordering guarantee described in {@link com.forvmom.core.producer.BookingEventProducer}.
 */
@Configuration
public class KafkaConfig {

    static final String DEAD_LETTER_TOPIC = "core-dlt";
    private static final int PRODUCER_DELIVERY_TIMEOUT_MILLISECONDS = 30_000;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.topics.booking-requested}")
    private String bookingRequestedTopic;

    @Value("${kafka.topics.booking-confirmed}")
    private String bookingConfirmedTopic;

    @Value("${kafka.topics.booking-failed}")
    private String bookingFailedTopic;

    // ==================== PRODUCER CONFIG ====================

    /**
     * Producer factory with JSON value serialization and durable-write settings.
     *
     * @return producer factory used by {@link #kafkaTemplate()}
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();

        // Basic connection
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Serializers (JSON)
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Useful reliability settings
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,
                PRODUCER_DELIVERY_TIMEOUT_MILLISECONDS);

        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * Shared template used both for publishing domain events and as the sink for
     * dead-lettered records.
     *
     * @return the Kafka template
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ==================== CONSUMER CONFIG ====================

    /**
     * Consumer factory for the {@code core-group} consumer group.
     *
     * <p>
     * {@code auto.offset.reset=earliest} means a newly deployed instance replays
     * from the start of the topic rather than skipping events it never saw — safe
     * here because every core consumer is idempotent.
     *
     * @return consumer factory used by {@link #kafkaListenerContainerFactory}
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerProperties());
    }

    /**
     * Builds consumer settings shared by the listener factory and focused tests.
     *
     * <p>The error-handling wrappers convert malformed key/value payloads into
     * recoverable listener failures so poison records follow the configured DLT path.
     */
    Map<String, Object> consumerProperties() {
        Map<String, Object> config = new HashMap<>();

        // Basic connection
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "core-group");

        // Deserializers (JSON)
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // Trust our events package (BookingRequestEvent and inner classes)
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.forvmom.common.dto.events");

        // Useful consumer settings
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return config;
    }

    /**
     * Listener container factory referenced by name from every {@code @KafkaListener}
     * in the core service.
     *
     * <p>
     * Concurrency is deliberately 1: with single-partition topics extra consumer
     * threads would idle, and raising it is only meaningful once partition count
     * is raised too.
     *
     * @param consumerFactory factory supplying the underlying consumers
     * @param kafkaTemplate   template used to publish records to {@code core-dlt}
     * @return the configured listener container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(1); // Start with 1, increase later if needed

        // Manual acknowledgment
        factory.getContainerProperties().setAckMode(
                ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Simple error handler with retry and DLQ
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                KafkaConfig::deadLetterDestination);

        // Retry 3 times with 1 second delay
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));

        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    /**
     * Routes exhausted records from every source topic to the shared core DLT.
     * Partition {@code -1} lets Kafka choose a valid destination partition.
     */
    static TopicPartition deadLetterDestination(
            ConsumerRecord<?, ?> record,
            Exception exception
    ) {
        return new TopicPartition(DEAD_LETTER_TOPIC, -1);
    }

    // ==================== TOPICS ====================

    /**
     * Outbound topic carrying the enriched {@code BookingRequestEvent} to the
     * Booking service.
     *
     * @return topic definition, auto-created on startup
     */
    @Bean
    public NewTopic bookingRequestedTopic() {
        return TopicBuilder.name(bookingRequestedTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    /**
     * Inbound topic on which the Booking service reports successful confirmations.
     *
     * @return topic definition, auto-created on startup
     */
    @Bean
    public NewTopic bookingConfirmedTopic() {
        return TopicBuilder.name(bookingConfirmedTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    /**
     * Inbound topic that triggers inventory compensation in
     * {@link com.forvmom.core.idempotency.event.consumer.BookingFailedConsumer}.
     *
     * @return topic definition, auto-created on startup
     */
    @Bean
    public NewTopic bookingFailedTopic() {
        return TopicBuilder.name(bookingFailedTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    /**
     * Dead-letter topic. Records land here after 3 failed delivery attempts and
     * require manual inspection — nothing consumes it automatically.
     *
     * @return topic definition, auto-created on startup
     */
    @Bean
    public NewTopic deadLetterTopic() {
        return TopicBuilder.name(DEAD_LETTER_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}