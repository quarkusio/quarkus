package io.quarkus.jfr.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class RequestIdTest {

    @Test
    public void testRequestWithoutRequestId() {
        given()
                .when().get("/requestId")
                .then()
                .statusCode(200)
                .body("traceId", matchesRegex("[0-9a-f]{32}"))
                .body("spanId", nullValue());
    }
}
