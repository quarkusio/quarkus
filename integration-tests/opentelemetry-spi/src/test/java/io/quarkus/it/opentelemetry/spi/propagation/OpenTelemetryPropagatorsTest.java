package io.quarkus.it.opentelemetry.spi.propagation;

import static io.restassured.RestAssured.given;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class OpenTelemetryPropagatorsTest {

    @Test
    void test() throws NoSuchFieldException, IllegalAccessException {
        given()
                .contentType("application/json")
                .when().get("/export/propagation")
                .then()
                .statusCode(200)
                .body("", Matchers.containsInAnyOrder(
                        //default baggage will be missing
                        //W3C headers:
                        "traceparent",
                        "tracestate",
                        //XRAY headers:
                        "X-Amzn-Trace-Id"));
    }
}
