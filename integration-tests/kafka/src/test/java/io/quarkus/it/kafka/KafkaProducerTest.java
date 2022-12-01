package io.quarkus.it.kafka;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.mapper.ObjectMapperType;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerTask;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

@QuarkusTestResource(KafkaCompanionResource.class)
@QuarkusTest
public class KafkaProducerTest {

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @BeforeAll
    public static void configureMapper() {
        // We have JSON-B and Jackson around, we want to ensure REST Assured uses Jackson and not JSON-B
        RestAssured.config = RestAssured.config.objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                .defaultObjectMapperType(ObjectMapperType.JACKSON_2));
    }

    @Test
    public void test() {
        ConsumerTask<Integer, String> consume = companion.consume(Integer.class, String.class)
                .withGroupId("test")
                .withAutoCommit()
                .fromTopics("test", 1);
        RestAssured.with().body("hello").post("/kafka");
        ConsumerRecord<Integer, String> records = consume.awaitCompletion().getFirstRecord();
        Assertions.assertEquals(records.key(), (Integer) 0);
        Assertions.assertEquals(records.value(), "hello");
    }

    @Test
    public void health() {
        RestAssured.when().get("/q/health/ready").then()
                .body("status", is("UP"),
                        "checks.status", containsInAnyOrder("UP"),
                        "checks.name", containsInAnyOrder("Kafka connection health check"));
    }

    @Test
    public void metrics() {
        // Look for kafka producer metrics (add .log().all() to examine what they are
        RestAssured.when().get("/q/metrics").then()
                .statusCode(200)
                .body(containsString("kafka_producer_"));
    }
}
