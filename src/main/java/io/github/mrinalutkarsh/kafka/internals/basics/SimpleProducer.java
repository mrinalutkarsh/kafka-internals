package io.github.mrinalutkarsh.kafka.internals.basics;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.time.Instant;
import java.util.Properties;
import java.util.UUID;

/**
 * SimpleProducer
 * --------------
 * A minimal Kafka producer used to understand:
 *
 * 1. How messages are sent to Kafka
 * 2. How keys affect partitioning
 * 3. Where ordering guarantees come from
 * 4. How offsets are assigned
 *
 * This producer sends ORDER_CREATED events using:
 * - key   = orderId
 * - value = simple String payload (Avro comes later)
 */
public class SimpleProducer {

    /**
     * Topic name.
     * Must exist before producing.
     */
    private static final String TOPIC_NAME = "order-events";

    public static void main(String[] args) {

        // ------------------------------------------------------------------
        // Step 1: Load producer configuration
        // ------------------------------------------------------------------
        Properties props = KafkaConfig.producerProps();
        // props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);

        // Create KafkaProducer
        try (KafkaProducer<String, String> producer =
                     new KafkaProducer<>(props)) {

            // ------------------------------------------------------------------
            // Step 2: Send a few ORDER_CREATED events
            // ------------------------------------------------------------------
            for (int i = 1; i <= 5; i++) {

                // Unique order ID (used as Kafka message key)
                String orderId = "order-" + UUID.randomUUID();

                // Simple payload (later replaced by Avro)
                String value = String.format(
                        "ORDER_CREATED | orderId=%s | amount=%d | time=%s",
                        orderId,
                        i * 100,
                        Instant.now()
                );

                /**
                 * ProducerRecord
                 * --------------
                 * new ProducerRecord<>(topic, key, value)
                 *
                 * Key is CRITICAL:
                 * - Same key → same partition
                 * - Same partition → ordering guaranteed
                 */
                ProducerRecord<String, String> record =
                        new ProducerRecord<>(TOPIC_NAME, orderId, value);

                // ------------------------------------------------------------------
                // Step 3: Send asynchronously with callback
                // ------------------------------------------------------------------
                // ❗ Important
                // send() does NOT send over the network
                // It adds the record to an in-memory buffer
                producer.send(record, new ProducerCallback(orderId));

                // Small delay for readability in logs
                sleep(500);
            }

            // ------------------------------------------------------------------
            // Step 4: Flush ensures all messages are actually sent
            // ------------------------------------------------------------------
            producer.flush();
        }

        System.out.println("Producer finished sending messages.");
    }

    /**
     * Callback invoked when Kafka acknowledges the record.
     *
     * This is where Kafka internals become visible:
     * - partition
     * - offset
     * - timestamp
     */
    private static class ProducerCallback implements Callback {

        private final String orderId;

        ProducerCallback(String orderId) {
            this.orderId = orderId;
        }

        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {

            if (exception != null) {
                System.err.println("Error producing message for orderId="
                        + orderId);
                exception.printStackTrace();
                return;
            }

            System.out.printf(
                    "Produced event | orderId=%s | topic=%s | partition=%d | offset=%d | timestamp=%d%n",
                    orderId,
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset(),
                    metadata.timestamp()
            );
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }
}
