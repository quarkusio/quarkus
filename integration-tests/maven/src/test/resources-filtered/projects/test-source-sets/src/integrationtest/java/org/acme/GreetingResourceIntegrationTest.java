package org.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class GreetingResourceIntegrationTest {

  @Test
  public void testHelloEndpoint() {
    // @formatter: off
    given()
        .when().get("/hello")
        .then()
          .statusCode(200)
          .body(is(GreetingResource.HELLO));
    // @formatter: on
  }
}