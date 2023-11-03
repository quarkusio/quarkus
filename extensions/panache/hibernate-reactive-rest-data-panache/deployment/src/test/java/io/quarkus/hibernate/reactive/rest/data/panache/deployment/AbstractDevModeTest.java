package io.quarkus.hibernate.reactive.rest.data.panache.deployment;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

public abstract class AbstractDevModeTest {

    @Test
    void testGet() {
        when().get("/items/1")
                .then().statusCode(200);
    }

    @Test
    void testCreate() {
        Response response = given().accept("application/json")
                .and().contentType("application/json")
                .and().body("{\"name\": \"test-simple\", \"collection\": {\"id\": \"full\"}}")
                .when().post("/items")
                .thenReturn();
        assertThat(response.getStatusCode()).isEqualTo(201);
    }
}
