package io.quarkus.it.logging.minlevel.set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Test verifies that a min-level override that goes below the default min-level is applied correctly.
 */
@QuarkusTest
@WithTestResource(value = SetRuntimeLogLevels.class, restrictToAnnotatedClass = false)
public class LoggingMinLevelBelowChildTest {

    @Test
    public void testTrace() {
        given()
                .when().get("/log/below/child/trace")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

}
