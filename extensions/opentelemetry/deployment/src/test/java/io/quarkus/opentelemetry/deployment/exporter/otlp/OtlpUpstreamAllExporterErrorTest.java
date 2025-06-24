package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.test.QuarkusUnitTest;

public class OtlpUpstreamAllExporterErrorTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.traces.exporter", "otlp")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "true")
            .overrideConfigKey("quarkus.otel.metrics.exporter", "otlp")
            .overrideConfigKey("quarkus.otel.logs.enabled", "true")
            .overrideConfigKey("quarkus.otel.logs.exporter", "otlp")
            .assertException(t -> {
                assertEquals(DeploymentException.class, t.getClass());
                Assertions.assertTrue(t.getMessage().contains(
                        "io.quarkus.runtime.configuration.ConfigurationException: " +
                                "OpenTelemetry exporter set to 'otlp' but upstream dependencies not found"));
            });

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void testOpenTelemetryButNoBatchSpanProcessor() {
        Assertions.fail("Test should not be run as deployment should fail with: " +
                "OpenTelemetry exporter set to 'otlp' but upstream dependencies not found... ");
    }
}
