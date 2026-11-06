package io.quarkus.opentelemetry.runtime.exporter.otlp;

import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfig.DEFAULT_HTTP_BASE_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.quarkus.opentelemetry.runtime.config.runtime.exporter.*;
import io.smallrye.config.ConfigInstanceBuilder;

class OtlpExporterProviderTest {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    @Test
    public void resolveTraceEndpoint_newWins() {
        assertEquals("http://localhost:2222/",
                OTelExporterRecorder.resolveTraceEndpoint(createOtlpExporterRuntimeConfig(
                        "http://localhost:1111/",
                        "http://localhost:2222/")));
    }

    @Test
    public void resolveTraceEndpoint_globalWins() {
        assertEquals("http://localhost:1111/",
                OTelExporterRecorder.resolveTraceEndpoint(createOtlpExporterRuntimeConfig(
                        "http://localhost:1111/",
                        DEFAULT_HTTP_BASE_URI)));
    }

    @Test
    public void resolveTraceEndpoint_legacyTraceWins() {
        assertEquals("http://localhost:2222/",
                OTelExporterRecorder.resolveTraceEndpoint(createOtlpExporterRuntimeConfig(
                        DEFAULT_HTTP_BASE_URI,
                        "http://localhost:2222/")));
    }

    @Test
    public void resolveTraceEndpoint_legacyGlobalWins() {
        assertEquals(DEFAULT_HTTP_BASE_URI,
                OTelExporterRecorder.resolveTraceEndpoint(createOtlpExporterRuntimeConfig(
                        DEFAULT_HTTP_BASE_URI,
                        null)));
    }

    @Test
    public void resolveTraceEndpoint_testIsSet() {
        assertEquals(DEFAULT_HTTP_BASE_URI,
                OTelExporterRecorder.resolveTraceEndpoint(createOtlpExporterRuntimeConfig(
                        null,
                        null)));
    }

    @Test
    public void resolveMetricEndpoint_newWins() {
        assertEquals("http://localhost:2222/",
                OTelExporterRecorder.resolveMetricEndpoint(createOtlpExporterRuntimeConfig(
                        "http://localhost:1111/",
                        "http://localhost:2222/")));
    }

    @Test
    public void resolveMetricEndpoint_globalWins() {
        assertEquals("http://localhost:1111/",
                OTelExporterRecorder.resolveMetricEndpoint(createOtlpExporterRuntimeConfig(
                        "http://localhost:1111/",
                        DEFAULT_HTTP_BASE_URI)));
    }

    @Test
    public void resolveMetricEndpoint_legacyTraceWins() {
        assertEquals("http://localhost:2222/",
                OTelExporterRecorder.resolveMetricEndpoint(createOtlpExporterRuntimeConfig(
                        DEFAULT_HTTP_BASE_URI,
                        "http://localhost:2222/")));
    }

    @Test
    public void resolveMetricEndpoint_legacyGlobalWins() {
        assertEquals(DEFAULT_HTTP_BASE_URI,
                OTelExporterRecorder.resolveMetricEndpoint(createOtlpExporterRuntimeConfig(
                        DEFAULT_HTTP_BASE_URI,
                        null)));
    }

    @Test
    public void resolveMetricEndpoint_testIsSet() {
        assertEquals(DEFAULT_HTTP_BASE_URI,
                OTelExporterRecorder.resolveMetricEndpoint(createOtlpExporterRuntimeConfig(
                        null,
                        null)));
    }

    @Test
    public void resolveLogsEndpoint_newWins() {
        assertEquals("http://localhost:2222/",
                OTelExporterRecorder.resolveLogsEndpoint(createOtlpExporterRuntimeConfig(
                        "http://localhost:1111/",
                        "http://localhost:2222/")));
    }

    @Test
    public void resolveLogsEndpoint_globalWins() {
        assertEquals("http://localhost:1111/",
                OTelExporterRecorder.resolveLogsEndpoint(createOtlpExporterRuntimeConfig(
                        "http://localhost:1111/",
                        DEFAULT_HTTP_BASE_URI)));
    }

    @Test
    public void resolveLogsEndpoint_legacyTraceWins() {
        assertEquals("http://localhost:2222/",
                OTelExporterRecorder.resolveLogsEndpoint(createOtlpExporterRuntimeConfig(
                        DEFAULT_HTTP_BASE_URI,
                        "http://localhost:2222/")));
    }

    @Test
    public void resolveLogsEndpoint_legacyGlobalWins() {
        assertEquals(DEFAULT_HTTP_BASE_URI,
                OTelExporterRecorder.resolveLogsEndpoint(createOtlpExporterRuntimeConfig(
                        DEFAULT_HTTP_BASE_URI,
                        null)));
    }

    @Test
    public void resolveLogsEndpoint_testIsSet() {
        assertEquals(DEFAULT_HTTP_BASE_URI,
                OTelExporterRecorder.resolveLogsEndpoint(createOtlpExporterRuntimeConfig(
                        null,
                        null)));
    }

    private OtlpExporterRuntimeConfig createOtlpExporterRuntimeConfig(String exporterGlobal, String newTrace) {
        ConfigInstanceBuilder<OtlpExporterRuntimeConfig> config = ConfigInstanceBuilder
                .forInterface(OtlpExporterRuntimeConfig.class)
                .with(OtlpExporterConfig::timeout, DEFAULT_TIMEOUT)
                .with(OtlpExporterConfig::proxyOptions, disabledProxy())
                .with(OtlpExporterRuntimeConfig::traces,
                        withEndpoint(ConfigInstanceBuilder.forInterface(OtlpExporterTracesConfig.class), newTrace))
                .with(OtlpExporterRuntimeConfig::metrics,
                        withEndpoint(ConfigInstanceBuilder.forInterface(OtlpExporterMetricsConfig.class), newTrace))
                .with(OtlpExporterRuntimeConfig::logs,
                        withEndpoint(ConfigInstanceBuilder.forInterface(OtlpExporterLogsConfig.class), newTrace));
        if (exporterGlobal != null) {
            config.withOptional(OtlpExporterConfig::endpoint, exporterGlobal);
        }
        return config.build();
    }

    private static <T extends OtlpExporterConfig> T withEndpoint(ConfigInstanceBuilder<T> config, String endpoint) {
        config.with(OtlpExporterConfig::timeout, DEFAULT_TIMEOUT)
                .with(OtlpExporterConfig::proxyOptions, disabledProxy());
        if (endpoint != null) {
            config.withOptional(OtlpExporterConfig::endpoint, endpoint);
        }
        return config.build();
    }

    private static OtlpExporterConfig.ProxyConfig disabledProxy() {
        return ConfigInstanceBuilder.forInterface(OtlpExporterConfig.ProxyConfig.class)
                .with(OtlpExporterConfig.ProxyConfig::enabled, false)
                .build();
    }
}
