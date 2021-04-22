package io.quarkus.it.kafka;

import static org.hamcrest.Matchers.containsString;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTestResource(KafkaTestResource.class)
@QuarkusTest
public class KafkaConsumerTest {

    public static Producer<Integer, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaTestResource.getBootstrapServers());
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "test-consumer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<Integer, String>(props);
    }

    @Test
    public void test() {
        Producer<Integer, String> producer = createProducer();
        producer.send(new ProducerRecord<>("test-consumer", 1, "hi world"));
        RestAssured.when().get("/kafka").then().body(Matchers.is("hi world"));
    }

    @Test
    public void metrics() {
        // Look for kafka consumer metrics (add .log().all() to examine what they are
        RestAssured.when().get("/q/metrics").then()
                .statusCode(200)
                .body(containsString("kafka_consumer_"));
    }
}
