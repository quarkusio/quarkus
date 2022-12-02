package io.quarkus.it.logging.minlevel.unset;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * This test verifies that a runtime log level can go above the default min-level,
 * and only messages at same runtime log level or above are shown.
 */
@QuarkusTest
@QuarkusTestResource(SetCategoryRuntimeLogLevels.class)
public class LoggingMinLevelAboveTest {

    @Test
    public void testNotInfo() {
        given()
                .when().get("/log/above/not-info")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    public void testWarn() {
        given()
                .when().get("/log/above/warn")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    public void testNotTrace() {
        given()
                .when().get("/log/above/not-trace")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

}
