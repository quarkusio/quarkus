package io.quarkus.micrometer.deployment.export;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that the Prometheus registry throws an exception when meters with the same name
 * but different types are registered, instead of silently ignoring the duplicate.
 * <p>
 * There is no longer an error while re-registering the same meter with the same type.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/52410">GitHub issue #52410</a>
 */
public class PrometheusRegistrationFailureTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setFlatClassPath(true)
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.export.prometheus.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.registry-enabled-default", "false")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .withEmptyApplication();

    @Inject
    PrometheusMeterRegistry promRegistry;

    @Test
    public void testDuplicateMeterWithDifferentTypeThrowsException() {
        // Register a counter
        Counter.builder("my_metric")
                .tag("env", "prod")
                .register(promRegistry);

        // Attempting to register a timer with the same name
        // should throw an IllegalArgumentException instead of being silently ignored
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Timer.builder("my_metric")
                    .tag("env", "prod")
                    .register(promRegistry);
        }, "Registering a meter with the same name but a different type should throw an exception");
    }
}
