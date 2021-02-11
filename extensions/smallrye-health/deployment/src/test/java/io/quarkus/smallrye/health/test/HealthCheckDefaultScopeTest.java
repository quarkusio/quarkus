package io.quarkus.smallrye.health.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

public class HealthCheckDefaultScopeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(NoScopeCheck.class));

    @Test
    public void testHealth() {
        // the health check does not set a content type so we need to force the parser
        try {
            RestAssured.defaultParser = Parser.JSON;
            when().get("/q/health/live").then()
                    .body("status", is("UP"),
                            "checks.status", contains("UP"),
                            "checks.name", contains("noScope"));
            when().get("/q/health/live").then()
                    .body("status", is("DOWN"),
                            "checks.status", contains("DOWN"),
                            "checks.name", contains("noScope"));
        } finally {
            RestAssured.reset();
        }
    }

    // No scope - @Singleton is used by default
    @Liveness
    static class NoScopeCheck implements HealthCheck {

        volatile int counter = 0;

        @Override
        public HealthCheckResponse call() {
            if (++counter > 1) {
                return HealthCheckResponse.builder().down().name("noScope").build();
            }
            return HealthCheckResponse.builder().up().name("noScope").build();
        }
    }

}
