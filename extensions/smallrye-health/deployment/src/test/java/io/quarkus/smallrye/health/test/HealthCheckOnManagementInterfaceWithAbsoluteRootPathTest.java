package io.quarkus.smallrye.health.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

class HealthCheckOnManagementInterfaceWithAbsoluteRootPathTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyCheck.class))
            .overrideConfigKey("quarkus.management.enabled", "true")
            .overrideConfigKey("quarkus.smallrye-health.root-path", "/sante");

    @Test
    void testHealth() {
        try {
            RestAssured.defaultParser = Parser.JSON;
            when().get("http://localhost:9001/sante/live").then()
                    .body("status", is("UP"),
                            "checks.status", contains("UP"),
                            "checks.name", contains("my-check"));
            when().get("http://localhost:9001/sante/live").then()
                    .body("status", is("DOWN"),
                            "checks.status", contains("DOWN"),
                            "checks.name", contains("my-check"));
        } finally {
            RestAssured.reset();
        }
    }

    @Liveness
    static class MyCheck implements HealthCheck {

        final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public HealthCheckResponse call() {
            if (counter.incrementAndGet() > 1) {
                return HealthCheckResponse.builder().down().name("my-check").build();
            }
            return HealthCheckResponse.builder().up().name("my-check").build();
        }
    }

}
