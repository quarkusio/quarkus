package io.quarkus.opentelemetry.exporter.otlp.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.opentelemetry.exporter.otlp.runtime.OtlpExporterRuntimeConfig;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.SmallRyeConfig;

public class OtlpExporterConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey(SmallRyeConfig.SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN, "false")// FIXME default config mapping
            .overrideConfigKey("otel.traces.exporter", "cdi")
            .overrideConfigKey("otel.exporter.otlp.traces.protocol", "http/protobuf")
            .overrideConfigKey("quarkus.opentelemetry.tracer.exporter.otlp.endpoint", "http://localhost ");

    @Inject
    OtlpExporterRuntimeConfig config;

    @Test
    void configTrimmedEndpoint() {
        assertTrue(config.traces().legacyEndpoint().isPresent());
        assertEquals("http://localhost", config.traces().legacyEndpoint().get().trim());
    }
}
