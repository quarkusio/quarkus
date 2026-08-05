package org.acme;

import jakarta.inject.Inject;
import io.smallrye.config.Config;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class ExampleResourceTest {
    @Inject
    Config config;

    @Test
    void helloEndpoint() {
        given()
          .when().get("/hello")
          .then()
             .statusCode(200)
             .body(is("Hello from App"));
    }

    @Test
    void testOnly() {
      given()
          .when().get("test-only")
          .then()
              .statusCode(200)
              .body(is("Test only"));
    }

    @Test
    void profile() {
        System.out.println("profiles: " + config.getProfiles());
        Assertions.assertEquals("test", config.getProfiles().get(0));
    }
}