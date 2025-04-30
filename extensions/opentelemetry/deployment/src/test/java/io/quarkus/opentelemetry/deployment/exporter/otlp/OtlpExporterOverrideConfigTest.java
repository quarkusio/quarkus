package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig;
import io.quarkus.test.QuarkusUnitTest;

public class OtlpExporterOverrideConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.traces.exporter", "cdi")
            .overrideConfigKey("quarkus.otel.exporter.otlp.protocol", "http/protobuf")
            .overrideConfigKey("quarkus.otel.exporter.otlp.endpoint", "http://localhost ")
            .overrideConfigKey("quarkus.otel.bsp.schedule.delay", "50")
            .overrideConfigKey("quarkus.otel.bsp.export.timeout", "PT1S");

    @Inject
    OtlpExporterRuntimeConfig config;

    @Test
    void config() {
        assertTrue(config.traces().protocol().isPresent());
        assertEquals("http/protobuf", config.traces().protocol().get().trim());
        assertTrue(config.traces().endpoint().isPresent());
        assertEquals("http://localhost", config.traces().endpoint().get().trim());
    }
}
