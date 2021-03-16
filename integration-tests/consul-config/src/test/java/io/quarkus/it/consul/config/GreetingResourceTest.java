package io.quarkus.it.consul.config;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class GreetingResourceTest {

    @Test
    void testGreeting() {
        given()
                .when().get("/greeting")
                .then()
                .statusCode(200)
                .body(is(getExpectedValue()));

    }

    protected String getExpectedValue() {
        return "Hello from application.properties";
    }
}
