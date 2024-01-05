package io.quarkus.logging.opentelemetry.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class LoggingResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/logging-opentelemetry")
                .then()
                .statusCode(200)
                .body(is("Hello logging-opentelemetry"));
    }
}
