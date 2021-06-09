package io.quarkus.it.kafka;

import static org.hamcrest.Matchers.*;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class KafkaSnappyProducerTest {

    public static KafkaConsumer<Integer, String> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaTestResource.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        KafkaConsumer<Integer, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("test"));
        return consumer;
    }

    @Test
    public void test() throws Exception {
        KafkaConsumer<Integer, String> consumer = createConsumer();
        RestAssured.with().body("hello").post("/kafka");
        ConsumerRecord<Integer, String> records = consumer.poll(Duration.ofMillis(10000)).iterator().next();
        Assertions.assertEquals(records.key(), (Integer) 0);
        Assertions.assertEquals(records.value(), "hello");
    }

    @Test
    public void health() throws Exception {
        RestAssured.when().get("/q/health/ready").then()
                .body("status", is("UP"),
                        "checks.status", containsInAnyOrder("UP"),
                        "checks.name", containsInAnyOrder("Kafka connection health check"));
    }

    @Test
    public void metrics() throws Exception {
        // Look for kafka producer metrics (add .log().all() to examine what they are
        RestAssured.when().get("/q/metrics").then()
                .statusCode(200)
                .body(containsString("kafka_producer_"));
    }
}
