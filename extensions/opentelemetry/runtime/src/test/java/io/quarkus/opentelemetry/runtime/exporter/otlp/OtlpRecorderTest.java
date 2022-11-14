package io.quarkus.opentelemetry.runtime.exporter.otlp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterTracesConfig;
import io.quarkus.runtime.LaunchMode;

class OtlpRecorderTest {

    @Test
    public void resolveEndpoint_legacyWins() {
        assertEquals("http://localhost:1111/",
                OtlpRecorder.resolveEndpoint(LaunchMode.NORMAL, createOtlpExporterRuntimeConfig(
                        "http://localhost:4317/",
                        "http://localhost:1111/",
                        "http://localhost:2222/")));
    }

    @Test
    public void resolveEndpoint_legacyTraceWins() {
        assertEquals("http://localhost:2222/",
                OtlpRecorder.resolveEndpoint(LaunchMode.NORMAL, createOtlpExporterRuntimeConfig(
                        "http://localhost:4317/",
                        null,
                        "http://localhost:2222/")));
    }

    @Test
    public void resolveEndpoint_legacyGlobalWins() {
        assertEquals("http://localhost:4317/",
                OtlpRecorder.resolveEndpoint(LaunchMode.NORMAL, createOtlpExporterRuntimeConfig(
                        "http://localhost:4317/",
                        null,
                        null)));
    }

    @Test
    public void resolveEndpoint_testIsSet() {
        assertEquals("http://localhost:4317/",
                OtlpRecorder.resolveEndpoint(LaunchMode.DEVELOPMENT, createOtlpExporterRuntimeConfig(
                        null,
                        null,
                        null)));
    }

    @Test
    public void resolveEndpoint_NothingSet() {
        assertEquals("",
                OtlpRecorder.resolveEndpoint(LaunchMode.NORMAL, createOtlpExporterRuntimeConfig(
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
