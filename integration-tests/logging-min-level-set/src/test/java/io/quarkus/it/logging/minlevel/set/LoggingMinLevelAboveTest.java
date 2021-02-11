package io.quarkus.it.logging.minlevel.set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Test that verifies that min-level can go higher than default min-level,
 * and this can be further tweaked at runtime to go to above to an even higher level.
 */
@QuarkusTest
@QuarkusTestResource(SetRuntimeLogLevels.class)
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

}
