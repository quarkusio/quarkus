package io.quarkus.micrometer.deployment.export;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.Counter;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that the Prometheus registry throws an exception when meters with the same name
 * but different tag keys are registered, instead of silently ignoring the duplicate.
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
    public void testDuplicateMeterWithDifferentTagsThrowsException() {
        // Register a counter with tag key "env"
        Counter.builder("my_counter")
                .tag("env", "prod")
                .register(promRegistry);

        // Attempting to register a counter with the same name but a different tag key
        // should throw an IllegalArgumentException instead of being silently ignored
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Counter.builder("my_counter")
                    .tag("region", "us-east")
                    .register(promRegistry);
        }, "Registering a meter with the same name but different tag keys should throw an exception");
    }
}
