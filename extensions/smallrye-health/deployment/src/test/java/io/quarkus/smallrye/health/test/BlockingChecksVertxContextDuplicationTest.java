package io.quarkus.smallrye.health.test;

import static org.hamcrest.Matchers.is;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

class BlockingChecksVertxContextDuplicationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ContextCaptureCheck1.class, ContextCaptureCheck2.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    void testBlockingChecksPropagateVertxContext() {
        try {
            RestAssured.defaultParser = Parser.JSON;
            RestAssured.when().get("/q/health").then()
                    .body("status", is("UP"),
                            "checks.size()", is(2));

            Assertions.assertNotEquals(ContextCaptureCheck1.capturedContext, ContextCaptureCheck2.capturedContext,
                    "Expected different contexts to be propagated into different blocking health checks");
        } finally {
            RestAssured.reset();
        }
    }

    @Liveness
    public static class ContextCaptureCheck1 implements HealthCheck {

        public static Context capturedContext = null;

        @Override
        public HealthCheckResponse call() {
            capturedContext = Vertx.currentContext();
            return HealthCheckResponse.up("ContextCaptureCheck1");
        }
    }

    @Liveness
    public static class ContextCaptureCheck2 implements HealthCheck {

        public static Context capturedContext = null;

        @Override
        public HealthCheckResponse call() {
            capturedContext = Vertx.currentContext();
            return HealthCheckResponse.up("ContextCaptureCheck2");
        }
    }
}
