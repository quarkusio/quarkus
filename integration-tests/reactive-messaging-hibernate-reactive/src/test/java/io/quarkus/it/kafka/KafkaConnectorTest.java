package io.quarkus.it.kafka;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;

@QuarkusTest
@QuarkusTestResource(KafkaCompanionResource.class)
public class KafkaConnectorTest {

    protected static final TypeRef<List<Fruit>> FRUIT_TYPE_REF = new TypeRef<List<Fruit>>() {
    };

    protected static final TypeRef<List<Pet>> PET_TYPE_REF = new TypeRef<List<Pet>>() {
    };

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

        await().untilAsserted(() -> Assertions.assertEquals(6, get("/kafka/fruits").as(FRUIT_TYPE_REF).size()));
    }

    @Test
    public void testPet() {
        await().untilAsserted(() -> Assertions.assertEquals(0, get("/kafka/pets").as(PET_TYPE_REF).size()));
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

        await().untilAsserted(() -> Assertions.assertEquals(6, get("/kafka/pets").as(PET_TYPE_REF).size()));
        await().untilAsserted(() -> Assertions.assertEquals(6, get("/kafka/pets-consumed").as(PET_TYPE_REF).size()));
    }

    @Test
    public void testExactlyOnceWithTransaction() {
        await().atMost(java.time.Duration.ofSeconds(30)).untilAsserted(() -> {
            List<String> processed = get("/kafka/exactly-once-fruit-processed").as(new TypeRef<List<String>>() {
            });
            Assertions.assertTrue(processed.size() >= 4, "Expected at least 4 processed, got " + processed.size());
        });
        await().atMost(java.time.Duration.ofSeconds(30)).untilAsserted(() -> {
            List<String> results = get("/kafka/exactly-once-fruit-results").as(new TypeRef<List<String>>() {
            });
            Assertions.assertTrue(results.size() >= 4, "Expected at least 4 results, got " + results.size());
            Assertions.assertTrue(results.contains("persisted-fruit-0"));
        });
        // Verify fruits were persisted to the database by the @WithTransaction method
        await().atMost(java.time.Duration.ofSeconds(30)).untilAsserted(() -> {
            List<?> fruits = get("/kafka/exactly-once-fruits").as(new TypeRef<List<?>>() {
            });
            Assertions.assertTrue(fruits.size() >= 4, "Expected at least 4 persisted fruits, got " + fruits.size());
        });
    }
}
