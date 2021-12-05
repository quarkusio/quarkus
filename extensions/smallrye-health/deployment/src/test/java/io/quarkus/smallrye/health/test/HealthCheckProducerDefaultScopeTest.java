package io.quarkus.smallrye.health.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

public class HealthCheckProducerDefaultScopeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HealthCheckProducers.class));

    @Test
    public void testHealth() {
        // the health check does not set a content type so we need to force the parser
        try {
            RestAssured.defaultParser = Parser.JSON;
            when().get("/q/health/ready").then()
                    .body("status", is("UP"),
                            "checks.status", contains("UP", "UP"),
                            "checks.name", containsInAnyOrder("alpha1", "bravo1"));
            when().get("/q/health/ready").then()
                    .body("status", is("UP"),
                            "checks.status", contains("UP", "UP"),
                            "checks.name", containsInAnyOrder("alpha1", "bravo2"));
        } finally {
            RestAssured.reset();
        }
    }

    static class HealthCheckProducers {

        static final AtomicInteger ALPHA_COUNTER = new AtomicInteger();
        static final AtomicInteger BRAVO_COUNTER = new AtomicInteger();

        // No scope - @Singleton is used by default
        @Readiness
        HealthCheck alpha() {
            int idx = ALPHA_COUNTER.incrementAndGet();
            return () -> HealthCheckResponse.builder().up().name("alpha" + idx).build();
        }

        @RequestScoped
        @Readiness
        HealthCheck bravo() {
            int idx = BRAVO_COUNTER.incrementAndGet();
            return () -> HealthCheckResponse.builder().up().name("bravo" + idx).build();
        }

    }

}
