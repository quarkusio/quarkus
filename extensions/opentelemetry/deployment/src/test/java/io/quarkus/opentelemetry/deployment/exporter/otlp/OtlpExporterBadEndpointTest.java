package io.quarkus.opentelemetry.deployment.exporter.otlp;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.test.QuarkusUnitTest;

public class OtlpExporterBadEndpointTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.traces.exporter", "cdi")
            .overrideConfigKey("quarkus.otel.exporter.otlp.endpoint", "httz://nada:zero")
            .setExpectedException(IllegalArgumentException.class);

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void failStart() {
        Assertions.fail("Test should not be run as deployment should fail");
    }
}
