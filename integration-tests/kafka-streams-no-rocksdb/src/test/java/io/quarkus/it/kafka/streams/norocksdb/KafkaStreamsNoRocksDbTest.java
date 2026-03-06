package io.quarkus.it.kafka.streams.norocksdb;

import static org.hamcrest.Matchers.containsString;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.http.HttpStatus;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Integration test verifying Kafka Streams works with in-memory stores
 * when {@code quarkus.kafka-streams.rocksdb.enabled=false}.
 * <p>
 * The topology is a simple word-count using exclusively in-memory state stores.
 */
@QuarkusTestResource(KafkaTestResource.class)
@QuarkusTest
public class KafkaStreamsNoRocksDbTest {

    @Test
    public void testWordCountWithInMemoryStores() throws Exception {
        // Wait for topics and verify health check is initially DOWN
        testHealthNotReady();

        // Produce messages
        produceMessages();

        // Consume word count results
        KafkaConsumer<String, Long> consumer = createConsumer();
        List<ConsumerRecord<String, Long>> records = poll(consumer, 3);

        // Verify word counts
        long helloCount = records.stream()
                .filter(r -> "hello".equals(r.key()))
                .mapToLong(ConsumerRecord::value)
                .max().orElse(0);
        long worldCount = records.stream()
                .filter(r -> "world".equals(r.key()))
                .mapToLong(ConsumerRecord::value)
                .max().orElse(0);

        Assertions.assertTrue(helloCount >= 2, "Expected at least 2 'hello' counts, got " + helloCount);
        Assertions.assertTrue(worldCount >= 1, "Expected at least 1 'world' count, got " + worldCount);

        // Verify interactive query works (in-memory store)
        testInteractiveQuery();

        // Verify health check is UP
        testHealthReady();

        // Stop pipeline before broker shuts down
        RestAssured.post("/wordcount/stop");
    }

    private void testHealthNotReady() {
        RestAssured.get("/q/health/ready").then()
                .statusCode(HttpStatus.SC_SERVICE_UNAVAILABLE)
                .rootPath("checks.find { it.name == 'Kafka Streams topics health check' }")
                .body("status", CoreMatchers.is("DOWN"))
                .body("data.missing_topics", containsString("no-rocksdb-input"));
    }

    private void testHealthReady() {
        RestAssured.get("/q/health/ready").then()
                .statusCode(HttpStatus.SC_OK)
                .rootPath("checks.find { it.name == 'Kafka Streams topics health check' }")
                .body("status", CoreMatchers.is("UP"));

        RestAssured.get("/q/health/live").then()
                .statusCode(HttpStatus.SC_OK)
                .rootPath("checks.find { it.name == 'Kafka Streams state health check' }")
                .body("status", CoreMatchers.is("UP"))
                .body("data.state", CoreMatchers.is("RUNNING"));
    }

    private void testInteractiveQuery() throws Exception {
        int attempts = 0;
        Long count = null;
        while (attempts < 50 && (count == null || count < 2)) {
            String result = RestAssured.when().get("/wordcount/hello").asString();
            if (result != null && !result.trim().isEmpty()) {
                count = Long.valueOf(result);
            }
            Thread.sleep(100);
            attempts++;
        }
        Assertions.assertNotNull(count, "Word count for 'hello' should not be null");
        Assertions.assertTrue(count >= 2, "Expected at least 2 for 'hello', got " + count);
    }

    private void produceMessages() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaTestResource.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>("no-rocksdb-input", "key1", "hello world"));
            producer.send(new ProducerRecord<>("no-rocksdb-input", "key2", "hello quarkus"));
            producer.flush();
        }
    }

    private KafkaConsumer<String, Long> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaTestResource.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "no-rocksdb-test-consumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, Long> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("no-rocksdb-output"));
        return consumer;
    }

    private List<ConsumerRecord<String, Long>> poll(KafkaConsumer<String, Long> consumer, int minRecords) {
        List<ConsumerRecord<String, Long>> result = new ArrayList<>();
        int attempts = 0;
        while (result.size() < minRecords && attempts < 30) {
            ConsumerRecords<String, Long> records = consumer.poll(Duration.ofMillis(1000));
            records.forEach(result::add);
            attempts++;
        }
        return result;
    }
}
