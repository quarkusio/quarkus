package io.quarkus.opentelemetry.deployment.traces;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.test.QuarkusUnitTest;

public class OpenTelemetryTracingDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.traces.enabled", "false")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "true")
            .overrideConfigKey("quarkus.otel.logs.enabled", "true");

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void testNoOpenTelemetry() {
        Assertions.assertNotNull(openTelemetry, "A OpenTelemetry instance must be available");
    }
}
