package io.quarkus.smallrye.health.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Startup;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;

public class StartedHealthCheckTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(StartupHC.class));

    @Test
    public void testStartup() {
        try {
            RestAssured.defaultParser = Parser.JSON;
            when().get("/q/health/started").then()
                    .body("status", is("UP"),
                            "checks.status", contains("UP"),
                            "checks.name", contains(StartupHC.class.getName()));
        } finally {
            RestAssured.reset();
        }
    }

    @Startup
    static class StartupHC implements HealthCheck {

        @Override
        public HealthCheckResponse call() {
            return HealthCheckResponse.up(StartupHC.class.getName());
        }
    }

}
