package io.quarkus.opentelemetry.deployment;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A tracer provider customizer registered through the {@link AutoConfigurationCustomizerProvider} SPI has no
 * declarative configuration equivalent. It must select the legacy engine and be invoked, as on the autoconfigure
 * path.
 */
public class SpiTracerProviderCustomizerLegacySdkTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TracerProviderCustomizerProvider.class)
                    .addAsResource(new StringAsset(TracerProviderCustomizerProvider.class.getName()),
                            "META-INF/services/" + AutoConfigurationCustomizerProvider.class.getName()))
            .overrideConfigKey("quarkus.otel.traces.exporter", "none")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "false")
            .overrideConfigKey("quarkus.otel.logs.enabled", "false")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false");

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void tracerProviderCustomizerIsInvoked() {
        // forces the synthetic OpenTelemetry bean
        assertNotNull(openTelemetry.getTracer("test"));
        assertTrue(TracerProviderCustomizerProvider.invoked,
                "A tracer provider customizer registered through the SPI must be invoked");
    }

    public static class TracerProviderCustomizerProvider implements AutoConfigurationCustomizerProvider {

        static volatile boolean invoked;

        @Override
        public void customize(AutoConfigurationCustomizer autoConfiguration) {
            autoConfiguration.addTracerProviderCustomizer((builder, config) -> {
                invoked = true;
                return builder;
            });
        }
    }
}
