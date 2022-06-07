package org.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class HelloEndpointIT {
    @Test
    public void testListAllFruits() {
        given().get("/hello")
                .then()
                .statusCode(200)
                .body(is("hello"));
    }
}
