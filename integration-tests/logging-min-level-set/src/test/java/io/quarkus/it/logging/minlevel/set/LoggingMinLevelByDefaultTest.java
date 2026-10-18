package io.quarkus.it.logging.minlevel.set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests that verify that changes to the default min-level are applied correctly.
 *
 * If unset, the default is INFO,
 * so this test verifies that when the default min-level is changed,
 * say to DEBUG, the code works as expected.
 */
@QuarkusTest
@QuarkusTestResource(SetRuntimeLogLevels.class)
public class LoggingMinLevelByDefaultTest {

    @Test
    public void testDebug() {
        given()
                .when().get("/log/bydefault/debug")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    public void testNotTrace() {
        given()
                .when().get("/log/bydefault/not-trace")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

}
