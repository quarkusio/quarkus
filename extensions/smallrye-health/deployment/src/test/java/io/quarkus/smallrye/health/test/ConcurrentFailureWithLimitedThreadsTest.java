package io.quarkus.smallrye.health.test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

class ConcurrentFailureWithLimitedThreadsTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SlowHealthCheck.class))
            .overrideRuntimeConfigKey("quarkus.thread-pool.core-threads", "2")
            .overrideRuntimeConfigKey("quarkus.thread-pool.max-threads", "2")
            .overrideRuntimeConfigKey("quarkus.thread-pool.queue-size", "1");

    @Test
    void rejectedHealthChecksShouldReturn503NotHang() throws Exception {
        int concurrentRequests = 5;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < concurrentRequests; i++) {
            futures.add(executor.submit(() -> RestAssured.given()
                    .when().get("/q/health/live")
                    .then().extract().statusCode()));
        }

        // All requests must complete within 5 seconds.
        // With the bug, rejected requests hang indefinitely (executeBlocking future ignored).
        // With the fix, rejected requests return 503 immediately.
        for (int i = 0; i < futures.size(); i++) {
            try {
                int status = futures.get(i).get(5, TimeUnit.SECONDS);
                assertTrue(status == 200 || status == 503,
                        "request " + i + " status expected 200 or 503 but was " + status);
            } catch (TimeoutException e) {
                fail("Request " + i + " hung instead of returning 503 — executeBlocking future was ignored");
            }
        }

        executor.shutdownNow();
    }

    @Liveness
    @ApplicationScoped
    public static class SlowHealthCheck implements HealthCheck {

        @Override
        public HealthCheckResponse call() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return HealthCheckResponse.up("slow-check");
        }
    }
}
