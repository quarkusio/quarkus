package io.quarkus.it.kafka;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.it.kafka.fruit.Fruit;
import io.quarkus.it.kafka.people.PeopleState;
import io.quarkus.it.kafka.people.Person;
import io.quarkus.it.kafka.pet.Pet;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

@QuarkusTest
@QuarkusTestResource(KafkaCompanionResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class KafkaConnectorTest {

    protected static final TypeRef<List<Fruit>> TYPE_REF = new TypeRef<List<Fruit>>() {
    };

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @Test
    public void testFruits() {
        given().body(new Fruit("apple")).contentType(ContentType.JSON).when().post("/kafka/fruits").then()
                .assertThat().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));
        given().body(new Fruit("banana")).contentType(ContentType.JSON).when().post("/kafka/fruits").then()
                .assertThat().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));
        given().body(new Fruit("peach")).contentType(ContentType.JSON).when().post("/kafka/fruits").then()
                .assertThat().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));
        given().body(new Fruit("orange")).contentType(ContentType.JSON).when().post("/kafka/fruits").then()
                .assertThat().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));
        given().body(new Fruit("cherry")).contentType(ContentType.JSON).when().post("/kafka/fruits").then()
                .assertThat().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));
        given().body(new Fruit("pear")).contentType(ContentType.JSON).when().post("/kafka/fruits").then()
                .assertThat().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));

        await().untilAsserted(() -> assertEquals(6, get("/kafka/fruits").as(TYPE_REF).size()));

        for (ConsumerRecord<String, String> record : companion
                .consumeStrings()
                .fromTopics("fruits-persisted", 6)
                .awaitCompletion()) {
            System.out.println(record);
        }
    }

    protected static final TypeRef<List<Person>> PERSON_TYPE_REF = new TypeRef<List<Person>>() {
    };

    @Test
    public void testPeople() {
        await().untilAsserted(() -> assertEquals(get("/kafka/people").as(PERSON_TYPE_REF).size(), 6));
        await().untilAsserted(() -> {
            PeopleState result = get("/kafka/people-state").as(PeopleState.class);
            Assertions.assertNotNull(result);
            assertTrue(result.offset >= 6);
            assertEquals("bob;alice;tom;jerry;anna;ken", result.getNames());
        });
    }

    protected static final TypeRef<List<Pet>> PET_TYPE_REF = new TypeRef<List<Pet>>() {
    };

    @Test
    public void testPets() {
        await().untilAsserted(() -> assertEquals(0, get("/kafka/pets").as(PET_TYPE_REF).size()));

        given().body("cat").contentType(ContentType.TEXT).when().post("/kafka/pets").then()
                .assertThat().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));
        given().body("dog").contentType(ContentType.TEXT).when().post("/kafka/pets").then()
                .assertThat().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));
        given().body("bad").contentType(ContentType.TEXT).when().post("/kafka/pets").then()
                .assertThat().statusCode(is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        given().body("mouse").contentType(ContentType.TEXT).when().post("/kafka/pets").then()
                .assertThat().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));
        given().body("rabbit").contentType(ContentType.TEXT).when().post("/kafka/pets").then()
                .assertThat().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));
        given().body("fish").contentType(ContentType.TEXT).when().post("/kafka/pets").then()
                .assertThat().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));
        given().body("hamster").contentType(ContentType.TEXT).when().post("/kafka/pets").then()
                .assertThat().statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));

        await().untilAsserted(() -> assertEquals(6, get("/kafka/pets").as(PET_TYPE_REF).size()));
        await().untilAsserted(() -> assertEquals(6, get("/kafka/pets-consumed").as(PET_TYPE_REF).size()));
    }

    @Test
    @Order(1)
    public void testExactlyOnceTransactional() {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            List<String> processed = get("/kafka/exactly-once-fruit-processed").as(new TypeRef<List<String>>() {
            });
            assertTrue(processed.size() >= 4, "Expected at least 4 processed, got " + processed.size());
        });
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            List<String> results = get("/kafka/exactly-once-fruit-results").as(new TypeRef<List<String>>() {
            });
            assertTrue(results.size() >= 4, "Expected at least 4 results, got " + results.size());
            assertTrue(results.contains("persisted-fruit-0"));
        });
        // Verify fruits were persisted to the database by the @Transactional method
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            List<?> fruits = get("/kafka/exactly-once-fruits").as(new TypeRef<List<?>>() {
            });
            assertTrue(fruits.size() >= 4, "Expected at least 4 persisted fruits, got " + fruits.size());
        });
    }

    @Test
    @Order(2)
    public void testExactlyOnceTransactionalDlq() {
        int initialCount = get("/kafka/exactly-once-fruits").as(new TypeRef<List<?>>() {
        }).size();

        // Send a message that will be routed to DLQ instead of the main output
        companion.produceStrings().fromRecords(new ProducerRecord<>("exactly-once-fruit-in", "dlq-key", "dlq-fruit"));

        // Verify the record arrives on the DLQ topic
        List<ConsumerRecord<String, String>> dlqRecords = companion
                .consumeStrings()
                .withOffsetReset("earliest")
                .withProp(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed")
                .fromTopics("exactly-once-fruit-dlq", 1)
                .awaitCompletion(Duration.ofSeconds(30))
                .getRecords();
        assertEquals(1, dlqRecords.size());
        assertEquals("dlq-fruit", dlqRecords.get(0).value());
        assertEquals(0, dlqRecords.get(0).partition());

        // Verify "dlq-fruit" was NOT persisted to the database
        List<?> fruits = get("/kafka/exactly-once-fruits").as(new TypeRef<List<?>>() {
        });
        assertEquals(initialCount, fruits.size(),
                "dlq-fruit should not have been persisted");

        // Verify it was processed (not skipped)
        List<String> processed = get("/kafka/exactly-once-fruit-processed").as(new TypeRef<List<String>>() {
        });
        assertTrue(processed.contains("dlq-fruit"),
                "dlq-fruit should have been processed");
    }

    @Test
    @Order(3)
    public void testExactlyOnceTransactionalRetryAfterFailure() {
        int initialCount = get("/kafka/exactly-once-fruits").as(new TypeRef<List<?>>() {
        }).size();

        // Send a message that will cause the processor to throw on first attempt, then succeed on retry
        companion.produceStrings().fromRecords(new ProducerRecord<>("exactly-once-fruit-in", "fail-key", "fail-fruit"));

        // The first attempt fails and rolls back; the retry succeeds
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            List<String> processed = get("/kafka/exactly-once-fruit-processed").as(new TypeRef<List<String>>() {
            });
            assertTrue(processed.contains("fail-fruit"),
                    "fail-fruit should have been processed on retry");
        });

        // Verify fail-fruit was persisted exactly once after the successful retry
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            List<?> fruits = get("/kafka/exactly-once-fruits").as(new TypeRef<List<?>>() {
            });
            assertEquals(initialCount + 1, fruits.size(),
                    "fail-fruit should have been persisted on retry");
        });
    }

}
