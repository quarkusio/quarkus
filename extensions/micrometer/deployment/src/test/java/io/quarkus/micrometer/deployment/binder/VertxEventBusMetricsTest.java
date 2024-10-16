package io.quarkus.micrometer.deployment.binder;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.mutiny.core.Vertx;

public class VertxEventBusMetricsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
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

    private Search getMeter(String name, String address) {
        return registry.find(name).tags("address", address);
    }

    @Test
    void testEventBusMetrics() {
        var bus = vertx.eventBus();
        bus.consumer("address").handler(m -> {
            // ignored
        });
        bus.consumer("address").handler(m -> {
            // ignored
        });
        bus.<String> consumer("rpc").handler(m -> m.reply(m.body().toUpperCase()));

        bus.send("address", "a");
        bus.publish("address", "b");
        String resp = bus.<String> requestAndAwait("rpc", "hello").body();
        Assertions.assertEquals("HELLO", resp);

        Assertions.assertEquals(1, getMeter("eventBus.sent", "address").counter().count());
        Assertions.assertEquals(1, getMeter("eventBus.sent", "rpc").counter().count());
        Assertions.assertEquals(1, getMeter("eventBus.sent", "rpc").counter().getId().getTags().size());
        Assertions.assertEquals(1, getMeter("eventBus.published", "address").counter().count());

        Assertions.assertEquals(2, getMeter("eventBus.handlers", "address").gauge().value());
        Assertions.assertEquals(1, getMeter("eventBus.handlers", "rpc").gauge().value());
        Assertions.assertEquals(1, getMeter("eventBus.handlers", "rpc").gauge().getId().getTags().size());

        Assertions.assertEquals(0, getMeter("eventBus.discarded", "address").gauge().value());
        Assertions.assertEquals(0, getMeter("eventBus.discarded", "rpc").gauge().value());
        Assertions.assertEquals(1, getMeter("eventBus.discarded", "rpc").gauge().getId().getTags().size());

        Assertions.assertEquals(3, getMeter("eventBus.delivered", "address").gauge().value());
        Assertions.assertEquals(1, getMeter("eventBus.delivered", "rpc").gauge().value());
        Assertions.assertEquals(1, getMeter("eventBus.delivered", "rpc").gauge().getId().getTags().size());
    }

}
