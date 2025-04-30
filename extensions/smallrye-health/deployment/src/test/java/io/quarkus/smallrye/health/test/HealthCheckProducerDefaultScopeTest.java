package io.quarkus.smallrye.health.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.RequestScoped;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

class HealthCheckProducerDefaultScopeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HealthCheckProducers.class));

    @Test
    void testHealth() {
        // the health check does not set a content type, so we need to force the parser
        try {
            RestAssured.defaultParser = Parser.JSON;
            when().get("/q/health/ready").then()
                    .header("cache-control", "no-store")
                    .body("status", is("UP"),
                            "checks.status", hasItems("UP", "UP"),
                            "checks.name", hasItems("alpha1", "bravo1"));
            when().get("/q/health/ready").then()
                    .header("cache-control", "no-store")
                    .body("status", is("UP"),
                            "checks.status", hasItems("UP", "UP"),
                            "checks.name", hasItems("alpha1", "bravo2"));
        } finally {
            RestAssured.reset();
        }
    }

    static class HealthCheckProducers {

        static final AtomicInteger ALPHA_COUNTER = new AtomicInteger();
        static final AtomicInteger BRAVO_COUNTER = new AtomicInteger();

        // No scope - @Singleton is used by default
        @Readiness
        @SuppressWarnings("unused")
        HealthCheck alpha() {
            int idx = ALPHA_COUNTER.incrementAndGet();
            return () -> HealthCheckResponse.builder().up().name("alpha" + idx).build();
        }

        @RequestScoped
        @Readiness
        @SuppressWarnings("unused")
        HealthCheck bravo() {
            int idx = BRAVO_COUNTER.incrementAndGet();
            return () -> HealthCheckResponse.builder().up().name("bravo" + idx).build();
        }

    }

}
