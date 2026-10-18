package io.quarkus.it.logging.minlevel.unset;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests that logging works as expected when min-level is default,
 * and the global log level is below it.
 */
@QuarkusTest
@QuarkusTestResource(SetGlobalRuntimeLogLevel.class)
public class LoggingMinLevelGlobalTest {

    @Test
    public void testInfo() {
        given()
                .when().get("/log/bydefault/info")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    public void testWarn() {
        given()
                .when().get("/log/bydefault/warn")
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
