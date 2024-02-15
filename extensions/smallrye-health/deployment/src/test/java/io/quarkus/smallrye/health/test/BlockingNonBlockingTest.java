package io.quarkus.smallrye.health.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Route;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.health.SmallRyeHealthReporter;
import io.smallrye.mutiny.Uni;

public class BlockingNonBlockingTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BlockingHealthCheck.class, Routes.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void testRegisterHealthOnBlockingThreadStep1() {
        // initial startup health blocking call on worker thread
        given()
                .when().get("/start-health")
                .then().statusCode(200);

        try {
            RestAssured.defaultParser = Parser.JSON;
            // repeat the call a few times since the block isn't always logged
            for (int i = 0; i < 3; i++) {
                RestAssured.when().get("/q/health").then()
                        .body("status", is("UP"),
                                "checks.status", contains("UP"),
                                "checks.name", contains("blocking"));
            }
        } finally {
            RestAssured.reset();
        }
    }

    @Liveness
    static final class BlockingHealthCheck implements HealthCheck {
        @Override
        public HealthCheckResponse call() {
            // await() is illegal on the executor thread
            Uni.createFrom().item(42).onItem().delayIt().by(Duration.ofMillis(10)).await().indefinitely();
            return HealthCheckResponse.up("blocking");
        }
    }

    @ApplicationScoped
    static final class Routes {

        @Inject
        SmallRyeHealthReporter smallRyeHealthReporter;

        @Route(path = "/start-health", methods = Route.HttpMethod.GET)
        @Blocking
        public String health() {
            return smallRyeHealthReporter.getHealth().toString();
        }
    }
}
