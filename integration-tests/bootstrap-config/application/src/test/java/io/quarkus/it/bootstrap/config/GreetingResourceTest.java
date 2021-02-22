package io.quarkus.it.bootstrap.config;

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
                .body(containsString("dummy2"));
    }

}
