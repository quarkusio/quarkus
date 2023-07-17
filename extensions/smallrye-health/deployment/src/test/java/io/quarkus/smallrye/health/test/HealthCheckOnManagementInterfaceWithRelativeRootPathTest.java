package io.quarkus.smallrye.health.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

public class HealthCheckOnManagementInterfaceWithRelativeRootPathTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyCheck.class))
            .overrideConfigKey("quarkus.management.enabled", "true")
            .overrideConfigKey("quarkus.management.root-path", "/management")
            .overrideConfigKey("quarkus.smallrye-health.root-path", "sante");

    @Test
    public void testHealth() {
        try {
            RestAssured.defaultParser = Parser.JSON;
            when().get("http://0.0.0.0:9001/management/sante/live").then()
                    .body("status", is("UP"),
                            "checks.status", contains("UP"),
                            "checks.name", containsInAnyOrder("my-check"));
            when().get("http://0.0.0.0:9001/management/sante/live").then()
                    .body("status", is("DOWN"),
                            "checks.status", contains("DOWN"),
                            "checks.name", containsInAnyOrder("my-check"));
        } finally {
            RestAssured.reset();
        }
    }

    @Liveness
    static class MyCheck implements HealthCheck {

        volatile int counter = 0;

        @Override
        public HealthCheckResponse call() {
            if (++counter > 1) {
                return HealthCheckResponse.builder().down().name("my-check").build();
            }
            return HealthCheckResponse.builder().up().name("my-check").build();
        }
    }

}
