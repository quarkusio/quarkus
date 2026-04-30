package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
public class HelloResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("/hello")
          .then()
             .statusCode(200)
             .body(containsString("Hello"));
    }

    @Test
    public void testEntityMetamodelExists() {
        given()
          .when().get("/hello/entity")
          .then()
             .statusCode(200)
             .body(containsString("true"));
    }
}
