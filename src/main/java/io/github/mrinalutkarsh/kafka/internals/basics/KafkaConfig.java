package io.github.mrinalutkarsh.kafka.internals.basics;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

/**
 * KafkaConfig
 * -----------
 * Central place for Kafka Producer and Consumer configuration.
 *
 * This class is intentionally verbose and explicit.
 * Every configuration here exists to teach Kafka internals,
 * not to hide defaults.
 */
public final class KafkaConfig {

    // ----------------------------------------------------------------------
    // Common
    // ----------------------------------------------------------------------

    /**
     * Bootstrap servers act as the initial connection point to the Kafka cluster.
     * The client fetches metadata (brokers, partitions, leaders) from here.
     *
     * NOTE:
     * - These are NOT the only brokers used
     * - They are just entry points
     */
    // Make producer Docker-friendly - Your producer must NOT use localhost anymore.
    public static final String BOOTSTRAP_SERVERS = "kafka:9092";

    private KafkaConfig() {
        // prevent instantiation
    }

    // ----------------------------------------------------------------------
    // Producer Configuration
    // ----------------------------------------------------------------------

    /**
     * Basic producer configuration.
     *
     * This producer is intentionally configured for:
     * - Strong durability
     * - Ordering guarantees
     * - Internals visibility
     */
    public static Properties producerProps() {
        Properties props = new Properties();

        // Where to connect initially
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);

        // Key & Value serialization
        // (Avro will replace value serializer later)
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName());

        /**
         * acks=all
         * -----------
         * Leader waits for all in-sync replicas (ISR) to acknowledge
         * before considering a write successful.
         *
         * This directly impacts:
         * - durability
         * - leader election safety
         */
        props.put(ProducerConfig.ACKS_CONFIG, "all");

        /**
         * retries
         * -------
         * Retry on transient failures.
         * Ordering is preserved because idempotence will be enabled later.
         */
        props.put(ProducerConfig.RETRIES_CONFIG, 3);

        /**
         * linger.ms
         * ----------
         * Artificial delay to batch records.
         * Helps demonstrate throughput vs latency tradeoff.
         */
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);

        return props;
    }

    // ----------------------------------------------------------------------
    // Consumer Configuration
    // ----------------------------------------------------------------------

    /**
     * Basic consumer configuration.
     *
     * Auto-commit is disabled intentionally to learn:
     * - offset management
     * - at-least-once processing
     */
    public static Properties consumerProps(String consumerGroupId) {
        Properties props = new Properties();

        // Where to connect initially
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);

        // Consumer group ID
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);

        // Key & Value deserialization
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());

        /**
         * enable.auto.commit = false
         * --------------------------------
         * Offsets will be committed manually.
         * This is REQUIRED to understand Kafka offset semantics.
         */
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        /**
         * auto.offset.reset
         * -----------------
         * earliest:
         * - If no committed offset exists, start from beginning.
         * - Useful for learning & replaying events.
         */
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        /**
         * max.poll.records
         * ----------------
         * Limits number of records per poll.
         * Makes offset commits and failures easier to observe.
         */
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);

        return props;
    }
}