package io.quarkus.it.resteasy.jackson;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class GreetingResourceTest {

    @Test
    void testEndpoint() {
        given()
                .when().get("/greeting")
                .then()
                .statusCode(200)
                .body(containsString("hello"))
                .body(containsString("2019-01-01"));
    }

    @Test
    void testConfig() {
        // test that configuration can be obtained from application.properties
        given()
                .when().get("/greeting/config")
                .then()
                .statusCode(200)
                .body(containsString("5000"));
    }

    @Test
    public void testAbstract() {
        given()
                .when().get("/abstract/inherited")
                .then()
                .statusCode(200)
                .body(containsString("concrete"));

    }

}
