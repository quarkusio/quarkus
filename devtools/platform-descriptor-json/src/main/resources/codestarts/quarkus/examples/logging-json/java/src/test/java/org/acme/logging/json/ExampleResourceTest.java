package org.acme.logging.json;

import io.quarkus.test.junit.QuarkusTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class ExampleResourceTest {

    @Test
    void testHelloEndpoint() {
        given()
          .when().get("/logging-json/")
          .then()
             .statusCode(200)
             .body(is("hello"));
    }

    @Test
    void testGoodbyeEndpoint() {
        given()
                .when().get("/logging-json/goodbye")
                .then()
                .statusCode(500)
                .body(is(Matchers.emptyString()));
    }

}