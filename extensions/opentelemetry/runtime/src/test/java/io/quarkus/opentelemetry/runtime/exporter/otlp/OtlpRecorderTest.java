package io.quarkus.opentelemetry.runtime.exporter.otlp;

import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig.Constants.DEFAULT_GRPC_BASE_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterTracesConfig;

class OtlpRecorderTest {

    @Test
    public void resolveEndpoint_legacyWins() {
        assertEquals("http://localhost:1111/",
                OtlpRecorder.resolveEndpoint(createOtlpExporterRuntimeConfig(
                        DEFAULT_GRPC_BASE_URI,
                        "http://localhost:1111/",
                        "http://localhost:2222/")));
    }

    @Test
    public void resolveEndpoint_newWins() {
        assertEquals("http://localhost:2222/",
                OtlpRecorder.resolveEndpoint(createOtlpExporterRuntimeConfig(
                        "http://localhost:1111/",
                        DEFAULT_GRPC_BASE_URI,
                        "http://localhost:2222/")));
    }

    @Test
    public void resolveEndpoint_globalWins() {
        assertEquals("http://localhost:1111/",
                OtlpRecorder.resolveEndpoint(createOtlpExporterRuntimeConfig(
                        "http://localhost:1111/",
                        DEFAULT_GRPC_BASE_URI,
                        DEFAULT_GRPC_BASE_URI)));
    }

    @Test
    public void resolveEndpoint_legacyTraceWins() {
        assertEquals("http://localhost:2222/",
                OtlpRecorder.resolveEndpoint(createOtlpExporterRuntimeConfig(
                        DEFAULT_GRPC_BASE_URI,
                        null,
                        "http://localhost:2222/")));
    }

    @Test
    public void resolveEndpoint_legacyGlobalWins() {
        assertEquals(DEFAULT_GRPC_BASE_URI,
                OtlpRecorder.resolveEndpoint(createOtlpExporterRuntimeConfig(
                        DEFAULT_GRPC_BASE_URI,
                        null,
                        null)));
    }

    @Test
    public void resolveEndpoint_testIsSet() {
        assertEquals(DEFAULT_GRPC_BASE_URI,
                OtlpRecorder.resolveEndpoint(createOtlpExporterRuntimeConfig(
                        null,
                        null,
                        null)));
    }

    private OtlpExporterRuntimeConfig createOtlpExporterRuntimeConfig(String exporterGlobal,
            String legacyTrace,
            String newTrace) {
        OtlpExporterRuntimeConfig otlpExporterRuntimeConfig = new OtlpExporterRuntimeConfig();
        otlpExporterRuntimeConfig.endpoint = Optional.ofNullable(exporterGlobal);
        otlpExporterRuntimeConfig.traces = new OtlpExporterTracesConfig();
        otlpExporterRuntimeConfig.traces.legacyEndpoint = Optional.ofNullable(legacyTrace);
        otlpExporterRuntimeConfig.traces.endpoint = Optional.ofNullable(newTrace);
        return otlpExporterRuntimeConfig;
    }
}
