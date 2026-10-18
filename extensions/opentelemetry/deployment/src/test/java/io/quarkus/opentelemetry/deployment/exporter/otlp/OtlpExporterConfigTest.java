package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig;
import io.quarkus.test.QuarkusExtensionTest;

public class OtlpExporterConfigTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.traces.exporter", "cdi")
            .overrideConfigKey("quarkus.otel.exporter.otlp.protocol", "wrong")
            .overrideConfigKey("quarkus.otel.exporter.otlp.traces.protocol", "grpc")
            .overrideConfigKey("quarkus.otel.exporter.otlp.traces.endpoint", "http://localhost ")
            .overrideConfigKey("quarkus.otel.exporter.otlp.metrics.protocol", "http/protobuf")
            .overrideConfigKey("quarkus.otel.exporter.otlp.metrics.endpoint", "http://localhost ")
            .overrideConfigKey("quarkus.otel.exporter.otlp.logs.protocol", "http/protobuf")
            .overrideConfigKey("quarkus.otel.exporter.otlp.logs.endpoint", "http://localhost ")
            .overrideConfigKey("quarkus.otel.bsp.schedule.delay", "50")
            .overrideConfigKey("quarkus.otel.bsp.export.timeout", "PT1S");

    @Inject
    OtlpExporterRuntimeConfig config;

    @Test
    void config() {
        assertTrue(config.traces().protocol().isPresent());
        assertEquals("grpc", config.traces().protocol().get().trim());
        assertTrue(config.traces().endpoint().isPresent());
        assertEquals("http://localhost", config.traces().endpoint().get().trim());

        assertTrue(config.metrics().protocol().isPresent());
        assertEquals("http/protobuf", config.metrics().protocol().get().trim());
        assertTrue(config.metrics().endpoint().isPresent());
        assertEquals("http://localhost", config.metrics().endpoint().get().trim());

        assertTrue(config.logs().protocol().isPresent());
        assertEquals("http/protobuf", config.logs().protocol().get().trim());
        assertTrue(config.logs().endpoint().isPresent());
        assertEquals("http://localhost", config.logs().endpoint().get().trim());
    }
}
