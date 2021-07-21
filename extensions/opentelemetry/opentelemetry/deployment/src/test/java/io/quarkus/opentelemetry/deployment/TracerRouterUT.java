package io.quarkus.opentelemetry.deployment;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test for continuos testing with OpenTelemetry
 */
@QuarkusTest
public class TracerRouterUT {

    @Test
    public void testTracer() {
        RestAssured.when().get("/tracer").then()
                .statusCode(200)
                .body(is("Hello Tracer!"));
    }
}
