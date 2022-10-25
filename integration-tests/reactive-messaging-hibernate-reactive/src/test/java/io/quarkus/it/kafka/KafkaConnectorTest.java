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

    protected static final TypeRef<List<Fruit>> TYPE_REF = new TypeRef<List<Fruit>>() {
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

        await().untilAsserted(() -> Assertions.assertEquals(6, get("/kafka/fruits").as(TYPE_REF).size()));
    }

}
