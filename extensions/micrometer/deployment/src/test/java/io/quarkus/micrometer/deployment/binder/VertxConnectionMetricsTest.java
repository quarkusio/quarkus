package io.quarkus.micrometer.deployment.binder;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.mutiny.ext.web.Router;

/**
 * Verify that we report connection metrics when the connection limit it set.
 */
public class VertxConnectionMetricsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            // Only allows 2 concurrent connections
            .overrideConfigKey("quarkus.http.limits.max-connections", "2")
            // Close the connection after 1s of inactivity, otherwise, the connection are kept open for 30min
            .overrideConfigKey("quarkus.http.idle-timeout", "1s")
            .withApplicationRoot(jar -> jar.addClasses(App.class));

    @Inject
    App app;

    private ExecutorService executor;
    private int concurrency;

    @BeforeEach
    public void init() {
        concurrency = 10;
        executor = Executors.newFixedThreadPool(10); // More than the connection limit
    }

    @AfterEach
    public void cleanup() {
        executor.shutdown();
    }

    @Test
    void testConnectionMetrics() throws InterruptedException {
        AtomicInteger rejected = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(concurrency);
        for (int i = 0; i < concurrency; i++) {
            executor.submit(() -> {
                try {
                    RestAssured.get("/ok").statusCode();
                } catch (Exception e) {
                    // RestAssured considers the rejection as an error.
                    rejected.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        Assertions.assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        Gauge max = Metrics.globalRegistry.find("vertx.http.connections.max").gauge();
        Gauge current = Metrics.globalRegistry.find("vertx.http.connections.current").gauge();
        Assertions.assertThat(max).isNotNull();
        Assertions.assertThat(current).isNotNull();

        Assertions.assertThat(max.value()).isEqualTo(2);

        // All requests are done, and connection closed (idle timeout)
        await().untilAsserted(() -> Assertions.assertThat(current.value()).isEqualTo(0));

        if (rejected.get() > 0) {
            Counter counter = Metrics.globalRegistry.find("vertx.http.connections.rejected").counter();
            Assertions.assertThat(counter).isNotNull();
            Assertions.assertThat(counter.count()).isGreaterThan(0);
        }
    }

    @ApplicationScoped
    public static class App {

        public void start(@Observes StartupEvent ev, Router router, io.vertx.core.Vertx vertx) {
            router.get("/ok").handler(rc -> {
                // Keep the connection open for 100ms.
                vertx.setTimer(250, l -> rc.endAndForget("ok"));
            });
        }

    }

}
