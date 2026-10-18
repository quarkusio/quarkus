package io.quarkus.it.logging.minlevel.set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * This test verifies that log levels are promoted to min-level when set below, even for subpackages.
 */
@QuarkusTest
@QuarkusTestResource(SetRuntimeLogLevels.class)
public class LoggingMinLevelPromoteSubTest {

    @Test
    public void testNotInfo() {
        given()
                .when().get("/log/promote/sub/not-info")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    public void testError() {
        given()
                .when().get("/log/promote/sub/error")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

}
