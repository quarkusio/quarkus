package org.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
class GreetingResourceIT {

    @Test
    void servesNamedNativeExecutable() {
        given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body(is("hello from the native application plugin build"));
    }
}
