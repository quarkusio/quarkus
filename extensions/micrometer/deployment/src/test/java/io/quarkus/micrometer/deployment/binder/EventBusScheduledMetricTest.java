package io.quarkus.micrometer.deployment.binder;

import static org.awaitility.Awaitility.await;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.mutiny.core.Vertx;

public class EventBusScheduledMetricTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .withEmptyApplication();

    final static SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @BeforeAll
    static void setRegistry() {
        Metrics.addRegistry(registry);
    }

    @AfterAll()
    static void removeRegistry() {
        Metrics.removeRegistry(registry);
    }

    @Inject
    Vertx vertx;

    @Test
    public void testEventBusScheduledMetric() {
        String address = "test.scheduled.address";

        vertx.eventBus().<String> consumer(address, message -> {
        });

        vertx.eventBus().send(address, "test message");

        await().untilAsserted(() -> {
            Counter counter = registry.find("eventBus.scheduled")
                    .tag("address", address)
                    .tag("side", "local")
                    .counter();
            Assertions.assertNotNull(counter, "eventBus.scheduled metric should be present");
            Assertions.assertTrue(counter.count() >= 1.0,
                    "Should have at least 1 scheduled message, got: " + counter.count());
        });
    }

    @Test
    public void testInternalAddressNotTracked() {
        String internalAddress = "__vertx.internal";

        vertx.eventBus().<String> consumer(internalAddress, message -> {
        });

        vertx.eventBus().send(internalAddress, "internal message");

        await().during(java.time.Duration.ofMillis(200)).untilAsserted(() -> {
            Counter counter = registry.find("eventBus.scheduled")
                    .tag("address", internalAddress)
                    .counter();
            Assertions.assertNull(counter, "Internal addresses should not be tracked");
        });
    }
}
