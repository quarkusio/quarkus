package io.quarkus;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class GoogleServicesResourcesTest {

    @Test
    public void testBigQuery() {
        given()
                .when().get("/bigquery")
                .then()
                .statusCode(200);
    }

    @Test
    public void testFirestore() {
        given()
                .when().get("/firestore")
                .then()
                .statusCode(200);
    }

    @Test
    public void testPubSub() {
        given()
                .when().get("/pubsub")
                .then()
                .statusCode(204);
    }

    @Test
    public void testSpanner() {
        given()
                .when().get("/spanner")
                .then()
                .statusCode(200);
    }

    @Test
    public void testStorage() {
        given()
                .when().get("/storage")
                .then()
                .statusCode(200);
    }

}
