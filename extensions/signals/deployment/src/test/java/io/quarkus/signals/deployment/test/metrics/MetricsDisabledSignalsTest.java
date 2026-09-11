package io.quarkus.signals.deployment.test.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.signals.runtime.metrics.MetricsSupport;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that no signal counters are registered when metrics are disabled via configuration, even though the
 * Micrometer extension is present.
 */
public class MetricsDisabledSignalsTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root
                    .addClasses(PingReceiver.class, Ping.class)
                    .addAsResource(new StringAsset("""
                            quarkus.signals.telemetry.metrics.enabled=false
                            """), "application.properties"))
            .setForcedDependencies(
                    List.of(Dependency.of("io.quarkus", "quarkus-micrometer-deployment", Version.getVersion())));

    @Inject
    MeterRegistry registry;

    @Inject
    Signal<Ping> ping;

    @BeforeAll
    static void addSimpleRegistry() {
        Metrics.globalRegistry.add(new SimpleMeterRegistry());
    }

    @Test
    public void testNoSignalMetricsWhenDisabled() {
        // Setup: signals metrics are turned off via quarkus.signals.telemetry.metrics.enabled=false, so the built-in
        // metrics enricher/interceptor are not registered - even though the Micrometer extension is present. The
        // emission itself must still work normally.
        // Expected: the request completes, but none of the signals counters exist.
        String result = ping.reactive().request(new Ping("hello"), String.class)
                .ifNoItem().after(Duration.ofSeconds(5)).fail()
                .await().indefinitely();
        assertEquals("hello", result);

        assertNull(registry.find(MetricsSupport.EMISSIONS).counter());
        assertNull(registry.find(MetricsSupport.RECEIVER_EXECUTIONS).counter());
    }

    record Ping(String value) {
    }

    @Singleton
    public static class PingReceiver {

        String ping(@Receives Ping ping) {
            return ping.value();
        }
    }
}
