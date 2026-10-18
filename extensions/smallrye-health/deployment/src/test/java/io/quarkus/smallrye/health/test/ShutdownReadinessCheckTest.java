package io.quarkus.smallrye.health.test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

class ShutdownReadinessCheckTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.shutdown.delay-enabled", "true");

    @Test
    void testShutdownHealthCheckInclusion() {
        try {
            RestAssured.defaultParser = Parser.JSON;
            RestAssured.when().get("/q/health/ready").then()
                    .body("status", is("UP"),
                            "checks.size()", is(1),
                            "checks.status", contains("UP"),
                            "checks.name", contains("Graceful Shutdown"));
        } finally {
            RestAssured.reset();
        }
    }
}
