package io.quarkus.opentelemetry.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * An {@link OpenTelemetry} bean provided by the application replaces the default one, and the default one is not created.
 */
public class ApplicationOpenTelemetryBeanTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(OpenTelemetryProducer.class, CountingCustomizer.class));

    static final OpenTelemetry APPLICATION_OPEN_TELEMETRY = OpenTelemetry.noop();

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void applicationBeanReplacesTheDefault() {
        assertThat(openTelemetry).isSameAs(APPLICATION_OPEN_TELEMETRY);
        assertThat(OpenTelemetryProducer.produced).hasValue(1);
        // the default bean runs every customizer when it is created
        assertThat(CountingCustomizer.CALLS).hasValue(0);
    }

    @Singleton
    public static class OpenTelemetryProducer {
        static final AtomicInteger produced = new AtomicInteger();

        @Produces
        @Singleton
        OpenTelemetry openTelemetry() {
            produced.incrementAndGet();
            return APPLICATION_OPEN_TELEMETRY;
        }
    }

    @SuppressWarnings("removal")
    @Singleton
    public static class CountingCustomizer implements AutoConfiguredOpenTelemetrySdkBuilderCustomizer {
        static final AtomicInteger CALLS = new AtomicInteger();

        @Override
        public void customize(AutoConfiguredOpenTelemetrySdkBuilder builder) {
            CALLS.incrementAndGet();
        }
    }
}
