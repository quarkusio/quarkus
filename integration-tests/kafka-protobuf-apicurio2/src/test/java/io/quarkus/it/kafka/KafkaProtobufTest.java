package io.quarkus.it.kafka;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.it.kafka.protobuf.Pet;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class KafkaProtobufTest {

    static final String APICURIO_PATH = "/protobuf/apicurio";
    static final String CONFLUENT_PATH = "/protobuf/confluent";

    @Test
    public void testApicurioProtobufProducer() {
        // Produce a message via REST endpoint (which serializes using Apicurio Protobuf)
        RestAssured.given()
                .header("content-type", "application/json")
                .body("{\"name\":\"neo\", \"color\":\"tricolor\"}")
                .post(APICURIO_PATH);

        // Consume the message via REST endpoint (which deserializes using Apicurio Protobuf)
        Pet retrieved = RestAssured.when().get(APICURIO_PATH).as(Pet.class);
        Assertions.assertEquals("neo", retrieved.getName());
        Assertions.assertEquals("tricolor", retrieved.getColor());
    }

    @Test
    public void testConfluentProtobufProducer() {
        // Produce a message via REST endpoint (which serializes using Confluent Protobuf)
        RestAssured.given()
                .header("content-type", "application/json")
                .body("{\"name\":\"luna\", \"color\":\"black\"}")
                .post(CONFLUENT_PATH);

        // Consume the message via REST endpoint (which deserializes using Confluent Protobuf)
        Pet retrieved = RestAssured.when().get(CONFLUENT_PATH).as(Pet.class);
        Assertions.assertEquals("luna", retrieved.getName());
        Assertions.assertEquals("black", retrieved.getColor());
    }
}
