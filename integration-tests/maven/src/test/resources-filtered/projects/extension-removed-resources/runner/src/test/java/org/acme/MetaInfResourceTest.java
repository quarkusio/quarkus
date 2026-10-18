package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class MetaInfResourceTest {

    @Test
    public void testMetaInfB() {
        given()
          .when().get("/meta-inf/b")
          .then()
             .statusCode(200)
             .body(is("b"));
    }

    @Test
    public void testMetaInfA() {
        given()
          .when().get("/meta-inf/a")
          .then()
             .statusCode(400);
    }
}