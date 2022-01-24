package io.quarkus.logging.network.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class LoggingSocketResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/logging-socket")
                .then()
                .statusCode(200)
                .body(is("Hello logging-socket"));
    }
}
