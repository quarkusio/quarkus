package io.quarkus.it.logging.minlevel.unset;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * This test verifies that log levels are promoted to min-level when set below the default min-level.
 * 
 * So given the default min-level is DEBUG,
 * so if log level is set to TRACE,
 * it will be automatically promoted to DEBUG.
 */
@QuarkusTest
@QuarkusTestResource(SetCategoryRuntimeLogLevels.class)
public class LoggingMinLevelPromoteTest {

    @Test
    public void testInfo() {
        given()
                .when().get("/log/promote/info")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    public void testWarn() {
        given()
                .when().get("/log/promote/warn")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    public void testNotTrace() {
        given()
                .when().get("/log/promote/not-trace")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

}
