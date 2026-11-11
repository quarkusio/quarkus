package io.quarkus.opentelemetry.deployment;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A deprecated {@link AutoConfiguredOpenTelemetrySdkBuilderCustomizer} supplied by a producer method is a lambda
 * at runtime, not an indexed implementor class, so the build-time Jandex scan cannot see it. It must still engage
 * the legacy engine and be invoked: silently ignoring a customizer is the failure mode the dispatch exists to
 * prevent.
 */
public class ProducedCustomizerLegacySdkTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(CustomizerProducer.class))
            .overrideConfigKey("quarkus.otel.traces.exporter", "none")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "false")
            .overrideConfigKey("quarkus.otel.logs.enabled", "false")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false");

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void producedCustomizerIsInvoked() {
        // forces the synthetic OpenTelemetry bean
        assertNotNull(openTelemetry.getTracer("test"));
        assertTrue(CustomizerProducer.invoked,
                "A customizer from a producer method must be invoked even though no implementor class is indexed");
    }

    @SuppressWarnings("removal")
    @ApplicationScoped
    public static class CustomizerProducer {

        static volatile boolean invoked;

        @Produces
        @Singleton
        AutoConfiguredOpenTelemetrySdkBuilderCustomizer customizer() {
            return builder -> invoked = true;
        }
    }
}
