package io.quarkus.opentelemetry.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * {@code otel.config.file} in the application configuration does not fail startup: autoconfigure read it from system
 * properties and environment variables only.
 */
public class OtelConfigFileInApplicationPropertiesTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(SdkAtStartup.class))
            .overrideConfigKey("otel.config.file", "otel-config.yaml")
            .overrideConfigKey("quarkus.otel.config.file", "otel-config.yaml")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false");

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void startsWithTheSdk() {
        assertThat(openTelemetry.getPropagators().getTextMapPropagator().fields()).contains("traceparent");
    }

    // Builds the SDK at startup, as an application that uses it does
    @Singleton
    public static class SdkAtStartup {

        void onStart(@Observes StartupEvent event, OpenTelemetry openTelemetry) {
        }
    }
}
