package io.quarkus.it.resteasy.jackson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class GreetingRecordResourceTest {

    @Test
    void testEndpoint() {
        given()
                .when().get("/greetingRecord")
                .then()
                .statusCode(200)
                .body(containsString("hello"))
                .body(containsString("2019-01-01"));
    }

}
