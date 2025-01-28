package io.quarkus.smallrye.health.test;

import static io.restassured.RestAssured.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class HealthObserversTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ChangingCheck.class, HealthObserver.class));

    @Test
    void testHealthObservers() {
        when().get("/q/health").then().statusCode(200); // UP (no status change)
        when().get("/q/health").then().statusCode(200); // UP (no status change)

        assertEquals(0, HealthObserver.healthChangeCounter,
                "Health change event should not have been received. Health status didn't change.");
        assertEquals(0, HealthObserver.readinessChangeCounter,
                "Readiness change event should not have been received. Readiness status didn't change.");
        assertEquals(0, HealthObserver.livenessChangeCounter,
                "Liveness change event should not have been received. Liveness status didn't change.");

        when().get("/q/health").then().statusCode(503); // DOWN (status change)

        assertEquals(1, HealthObserver.healthChangeCounter, "Health status change event not received");
        assertEquals(1, HealthObserver.readinessChangeCounter, "Readiness change event not received");
        assertEquals(0, HealthObserver.livenessChangeCounter,
                "Liveness change event should not have been received. Liveness status didn't change.");

        when().get("/q/health/ready").then().statusCode(200); // UP (status change)

        assertEquals(2, HealthObserver.healthChangeCounter, "Health status change event not received");
        assertEquals(2, HealthObserver.readinessChangeCounter, "Readiness change event not received");
        assertEquals(0, HealthObserver.livenessChangeCounter,
                "Liveness change event should not have been received. Liveness status didn't change.");

        when().get("/q/health/ready").then().statusCode(200); // UP (no status change)

        assertEquals(2, HealthObserver.healthChangeCounter,
                "Health change event should not have been received. Health status didn't change.");
        assertEquals(2, HealthObserver.readinessChangeCounter,
                "Readiness change event should not have been received. Readiness status didn't change.");
        assertEquals(0, HealthObserver.livenessChangeCounter,
                "Liveness change event should not have been received. Liveness status didn't change.");

        when().get("/q/health/ready").then().statusCode(503); // DOWN (status change)

        assertEquals(3, HealthObserver.healthChangeCounter, "Health status change event not received");
        assertEquals(3, HealthObserver.readinessChangeCounter, "Readiness change event not received");
        assertEquals(0, HealthObserver.livenessChangeCounter,
                "Liveness change event should not have been received. Liveness status didn't change.");
    }

    @Readiness
    public static class ChangingCheck implements HealthCheck {

        private static int counter = 1;

        @Override
        public HealthCheckResponse call() {
            return counter++ % 3 != 0 ? HealthCheckResponse.up(ChangingCheck.class.getName())
                    : HealthCheckResponse.down(ChangingCheck.class.getName());
        }
    }

}
