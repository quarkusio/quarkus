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
        OtlpExporterRuntimeConfig config = createOtlpExporterRuntimeConfig("http://localhost:1111/",
                "http://localhost:2222/");
        assertEquals("http://localhost:2222/",
                OtlpExporterEndpoints.resolveEndpoint(config, config.traces()));
    }

    @Test
    public void resolveTraceEndpoint_globalWins() {
        OtlpExporterRuntimeConfig config = createOtlpExporterRuntimeConfig("http://localhost:1111/",
                DEFAULT_HTTP_BASE_URI);
        assertEquals("http://localhost:1111/",
                OtlpExporterEndpoints.resolveEndpoint(config, config.traces()));
    }

    @Test
    public void resolveTraceEndpoint_legacyTraceWins() {
        OtlpExporterRuntimeConfig config = createOtlpExporterRuntimeConfig(DEFAULT_HTTP_BASE_URI,
                "http://localhost:2222/");
        assertEquals("http://localhost:2222/",
                OtlpExporterEndpoints.resolveEndpoint(config, config.traces()));
    }

    @Test
    public void resolveTraceEndpoint_legacyGlobalWins() {
        OtlpExporterRuntimeConfig config = createOtlpExporterRuntimeConfig(DEFAULT_HTTP_BASE_URI,
                null);
        assertEquals(DEFAULT_HTTP_BASE_URI,
                OtlpExporterEndpoints.resolveEndpoint(config, config.traces()));
    }

    @Test
    public void resolveTraceEndpoint_testIsSet() {
        OtlpExporterRuntimeConfig config = createOtlpExporterRuntimeConfig(null,
                null);
        assertEquals(DEFAULT_HTTP_BASE_URI,
                OtlpExporterEndpoints.resolveEndpoint(config, config.traces()));
    }

    @Test
    public void resolveMetricEndpoint_newWins() {
        OtlpExporterRuntimeConfig config = createOtlpExporterRuntimeConfig("http://localhost:1111/",
                "http://localhost:2222/");
        assertEquals("http://localhost:2222/",
                OtlpExporterEndpoints.resolveEndpoint(config, config.metrics()));
    }

    @Test
    public void resolveMetricEndpoint_globalWins() {
        OtlpExporterRuntimeConfig config = createOtlpExporterRuntimeConfig("http://localhost:1111/",
                DEFAULT_HTTP_BASE_URI);
        assertEquals("http://localhost:1111/",
                OtlpExporterEndpoints.resolveEndpoint(config, config.metrics()));
    }

    @Test
    public void resolveMetricEndpoint_legacyTraceWins() {
        OtlpExporterRuntimeConfig config = createOtlpExporterRuntimeConfig(DEFAULT_HTTP_BASE_URI,
                "http://localhost:2222/");
        assertEquals("http://localhost:2222/",
                OtlpExporterEndpoints.resolveEndpoint(config, config.metrics()));
    }

    @Test
    public void resolveMetricEndpoint_legacyGlobalWins() {
        OtlpExporterRuntimeConfig config = createOtlpExporterRuntimeConfig(DEFAULT_HTTP_BASE_URI,
                null);
        assertEquals(DEFAULT_HTTP_BASE_URI,
                OtlpExporterEndpoints.resolveEndpoint(config, config.metrics()));
    }

    @Test
    public void resolveMetricEndpoint_testIsSet() {
        OtlpExporterRuntimeConfig config = createOtlpExporterRuntimeConfig(null,
                null);
        assertEquals(DEFAULT_HTTP_BASE_URI,
                OtlpExporterEndpoints.resolveEndpoint(config, config.metrics()));
    }

    @Test
    public void resolveLogsEndpoint_newWins() {
        OtlpExporterRuntimeConfig config = createOtlpExporterRuntimeConfig("http://localhost:1111/",
                "http://localhost:2222/");
        assertEquals("http://localhost:2222/",
                OtlpExporterEndpoints.resolveEndpoint(config, config.logs()));
    }

    @Test
    public void resolveLogsEndpoint_globalWins() {
        OtlpExporterRuntimeConfig config = createOtlpExporterRuntimeConfig("http://localhost:1111/",
                DEFAULT_HTTP_BASE_URI);
        assertEquals("http://localhost:1111/",
                OtlpExporterEndpoints.resolveEndpoint(config, config.logs()));
    }

    @Test
    public void resolveLogsEndpoint_legacyTraceWins() {
        OtlpExporterRuntimeConfig config = createOtlpExporterRuntimeConfig(DEFAULT_HTTP_BASE_URI,
                "http://localhost:2222/");
        assertEquals("http://localhost:2222/",
                OtlpExporterEndpoints.resolveEndpoint(config, config.logs()));
    }

    @Test
    public void resolveLogsEndpoint_legacyGlobalWins() {
        OtlpExporterRuntimeConfig config = createOtlpExporterRuntimeConfig(DEFAULT_HTTP_BASE_URI,
                null);
        assertEquals(DEFAULT_HTTP_BASE_URI,
                OtlpExporterEndpoints.resolveEndpoint(config, config.logs()));
    }

    @Test
    public void resolveLogsEndpoint_testIsSet() {
        OtlpExporterRuntimeConfig config = createOtlpExporterRuntimeConfig(null,
                null);
        assertEquals(DEFAULT_HTTP_BASE_URI,
                OtlpExporterEndpoints.resolveEndpoint(config, config.logs()));
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
