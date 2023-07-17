package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class ApplicationConfigResourceTest {

    @Test
    public void testAppConfigEndpoint() {
        given()
          .when().get("/app-config")
          .then()
             .statusCode(200)
             .body(is("application:1.0.0-SNAPSHOT"));
    }

}