package io.quarkus.smallrye.health.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import io.smallrye.health.api.Wellness;

public class WellnessHealthCheckTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(WellnessHC.class));

    @Test
    public void testWellness() {
        try {
            RestAssured.defaultParser = Parser.JSON;
            when().get("/q/health/well").then()
                    .body("status", is("UP"),
                            "checks.status", contains("UP"),
                            "checks.name", contains(WellnessHC.class.getName()));
        } finally {
            RestAssured.reset();
        }
    }

    @Wellness
    static class WellnessHC implements HealthCheck {

        @Override
        public HealthCheckResponse call() {
            return HealthCheckResponse.up(WellnessHC.class.getName());
        }
    }

}
