package io.quarkus.smallrye.health.test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.stringContainsInOrder;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.health.api.AsyncHealthCheck;
import io.smallrye.mutiny.Uni;

public class AsyncDispatchedThreadTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(LivenessHealthCheckCapturingThread.class, ReadinessHealthCheckCapturingThread.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    public void check() {
        RestAssured.when().get("/q/health/live").then()
                .body("status", is("UP"),
                        "checks.status", contains("UP"),
                        "checks.name", contains("my-liveness-check"),
                        "checks.data.thread[0]", stringContainsInOrder("loop"),
                        "checks.data.thread[0]", not(stringContainsInOrder("executor-thread")),
                        "checks.data.request[0]", is(true));

        RestAssured.when().get("/q/health/ready").then()
                .body("status", is("UP"),
                        "checks.status", contains("UP"),
                        "checks.name", contains("my-readiness-check"),
                        "checks.data.thread[0]", stringContainsInOrder("loop"),
                        "checks.data.thread[0]", not(stringContainsInOrder("executor-thread")),
                        "checks.data.request[0]", is(true));
    }

    @ApplicationScoped
    @Liveness
    public static class LivenessHealthCheckCapturingThread implements AsyncHealthCheck {
        @Override
        public Uni<HealthCheckResponse> call() {
            return Uni.createFrom().item(HealthCheckResponse.named("my-liveness-check")
                    .up()
                    .withData("thread", Thread.currentThread().getName())
                    .withData("request", Arc.container().requestContext().isActive())
                    .build());
        }
    }

    @ApplicationScoped
    @Readiness
    public static class ReadinessHealthCheckCapturingThread implements AsyncHealthCheck {
        @Override
        public Uni<HealthCheckResponse> call() {
            return Uni.createFrom().item(HealthCheckResponse.named("my-readiness-check")
                    .up()
                    .withData("thread", Thread.currentThread().getName())
                    .withData("request", Arc.container().requestContext().isActive())
                    .build());
        }
    }

}
