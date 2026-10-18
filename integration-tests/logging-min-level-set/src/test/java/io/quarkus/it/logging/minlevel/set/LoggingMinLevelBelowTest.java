package io.quarkus.it.logging.minlevel.set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Test verifies that a min-level override that goes below the default min-level is applied correctly.
 */
@QuarkusTest
@QuarkusTestResource(SetRuntimeLogLevels.class)
public class LoggingMinLevelBelowTest {

    @Test
    public void testTrace() {
        given()
                .when().get("/log/below/trace")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

}
