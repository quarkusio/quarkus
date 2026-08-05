package io.quarkus.signals.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class SignalsResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/signals/foo")
                .then()
                .statusCode(200)
                .body(is("Hello foo!"));
        given()
                .when().get("/signals/count")
                .then()
                .statusCode(200)
                .body(is("1"));
    }
}
