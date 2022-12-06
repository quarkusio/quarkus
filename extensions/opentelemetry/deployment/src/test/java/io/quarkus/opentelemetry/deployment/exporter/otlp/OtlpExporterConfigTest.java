package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.opentelemetry.runtime.exporter.otlp.OtlpExporterConfig;
import io.quarkus.test.QuarkusUnitTest;

public class OtlpExporterConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.opentelemetry.tracer.exporter.otlp.endpoint", "http://localhost ");

    @Inject
    OtlpExporterConfig.OtlpExporterRuntimeConfig config;

    @Test
    void config() {
        assertTrue(config.endpoint.isPresent());
        assertEquals("http://localhost", config.endpoint.get());
    }
}
