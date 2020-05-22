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
                .body(containsString("[2019,1,1]"));
    }

    @Test
    void testYAMLEndpoint() {
        given()
                .when().get("/greeting/yaml")
                .then()
                .statusCode(200)
                .contentType("application/yaml")
                .body(containsString("message: \"hello\""));
    }

}
