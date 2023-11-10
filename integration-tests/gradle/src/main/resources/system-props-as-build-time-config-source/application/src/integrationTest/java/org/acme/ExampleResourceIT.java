package org.acme;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusIntegrationTest
public class ExampleResourceIT {

    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("/hello")
          .then()
             .statusCode(200)
             .body(is("hello cheburashka"));
    }

    @Test
    public void testRuntimeName() {
        given()
          .when().get("/hello/runtime-name")
          .then()
             .statusCode(200)
             .body(is("genadiy"));
    }
}