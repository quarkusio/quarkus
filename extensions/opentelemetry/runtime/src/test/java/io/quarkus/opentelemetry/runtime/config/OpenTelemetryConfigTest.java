package io.quarkus.opentelemetry.runtime.config;

import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.CompressionType.GZIP;
import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.CompressionType.NONE;
import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfig.DEFAULT_GRPC_BASE_URI;
import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfig.Protocol.GRPC;
import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfig.Protocol.HTTP_PROTOBUF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfigBuilder;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterMetricsConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterTracesConfig;
import io.quarkus.runtime.configuration.DurationConverter;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class OpenTelemetryConfigTest {
    @Test
    void fromBase() {
        Map<String, String> baseExporterConfig = new HashMap<>();
        baseExporterConfig.put("quarkus.otel.exporter.otlp.headers", "foo,bar");
        baseExporterConfig.put("quarkus.otel.exporter.otlp.compression", "none");
        baseExporterConfig.put("quarkus.otel.exporter.otlp.key-cert.keys", "keys");
        baseExporterConfig.put("quarkus.otel.exporter.otlp.key-cert.certs", "certs");
        baseExporterConfig.put("quarkus.otel.exporter.otlp.trust-cert.certs", "certs");
        baseExporterConfig.put("quarkus.otel.exporter.otlp.tls-configuration-name", "tls");
        baseExporterConfig.put("quarkus.otel.exporter.otlp.proxy-options.username", "naruto");
        baseExporterConfig.put("quarkus.otel.exporter.otlp.proxy-options.password", "hokage");
        baseExporterConfig.put("quarkus.otel.exporter.otlp.proxy-options.host", "konoha");
        baseExporterConfig.put("quarkus.otel.exporter.otlp.proxy-options.port", "9999");

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withCustomizers(builder -> new OtlpExporterConfigBuilder().configBuilder(builder))
                .withMapping(OtlpExporterRuntimeConfig.class)
                .withDefaultValues(baseExporterConfig)
                .build();

        OtlpExporterRuntimeConfig mapping = config.getConfigMapping(OtlpExporterRuntimeConfig.class);
        assertTrue(mapping.endpoint().isPresent());
        assertEquals(DEFAULT_GRPC_BASE_URI, mapping.endpoint().get());
        assertTrue(mapping.headers().isPresent());
        assertIterableEquals(List.of("foo", "bar"), mapping.headers().get());
        assertTrue(mapping.compression().isPresent());
        assertEquals(NONE, mapping.compression().get());
        assertEquals(DurationConverter.parseDuration("10s"), mapping.timeout());
        assertTrue(mapping.protocol().isPresent());
        assertEquals(GRPC, mapping.protocol().get());
        assertTrue(mapping.keyCert().keys().isPresent());
        assertIterableEquals(List.of("keys"), mapping.keyCert().keys().get());
        assertTrue(mapping.keyCert().certs().isPresent());
        assertIterableEquals(List.of("certs"), mapping.keyCert().certs().get());
        assertTrue(mapping.trustCert().certs().isPresent());
        assertIterableEquals(List.of("certs"), mapping.trustCert().certs().get());
        assertTrue(mapping.tlsConfigurationName().isPresent());
        assertEquals("tls", mapping.tlsConfigurationName().get());
        assertFalse(mapping.proxyOptions().enabled());
        assertTrue(mapping.proxyOptions().username().isPresent());
        assertEquals("naruto", mapping.proxyOptions().username().get());
        assertTrue(mapping.proxyOptions().password().isPresent());
        assertEquals("hokage", mapping.proxyOptions().password().get());
        assertTrue(mapping.proxyOptions().host().isPresent());
        assertEquals("konoha", mapping.proxyOptions().host().get());
        assertTrue(mapping.proxyOptions().port().isPresent());
        assertEquals(9999, mapping.proxyOptions().port().getAsInt());

        OtlpExporterTracesConfig traces = mapping.traces();
        assertTrue(traces.endpoint().isPresent());
        assertEquals(DEFAULT_GRPC_BASE_URI, traces.endpoint().get());
        assertTrue(traces.headers().isPresent());
        assertIterableEquals(List.of("foo", "bar"), traces.headers().get());
        assertTrue(traces.compression().isPresent());
        assertEquals(NONE, traces.compression().get());
        assertEquals(DurationConverter.parseDuration("10s"), traces.timeout());
        assertTrue(traces.protocol().isPresent());
        assertEquals(GRPC, traces.protocol().get());
        assertTrue(traces.keyCert().keys().isPresent());
        assertIterableEquals(List.of("keys"), traces.keyCert().keys().get());
        assertTrue(traces.keyCert().certs().isPresent());
        assertIterableEquals(List.of("certs"), traces.keyCert().certs().get());
        assertTrue(traces.trustCert().certs().isPresent());
        assertIterableEquals(List.of("certs"), traces.trustCert().certs().get());
        assertTrue(traces.tlsConfigurationName().isPresent());
        assertEquals("tls", traces.tlsConfigurationName().get());
        assertFalse(traces.proxyOptions().enabled());
        assertTrue(traces.proxyOptions().username().isPresent());
        assertEquals("naruto", traces.proxyOptions().username().get());
        assertTrue(traces.proxyOptions().password().isPresent());
        assertEquals("hokage", traces.proxyOptions().password().get());
        assertTrue(traces.proxyOptions().host().isPresent());
        assertEquals("konoha", traces.proxyOptions().host().get());
        assertTrue(traces.proxyOptions().port().isPresent());
        assertEquals(9999, traces.proxyOptions().port().getAsInt());

        OtlpExporterMetricsConfig metrics = mapping.metrics();
        assertTrue(metrics.endpoint().isPresent());
        assertEquals(DEFAULT_GRPC_BASE_URI, metrics.endpoint().get());
        assertTrue(metrics.headers().isPresent());
        assertIterableEquals(List.of("foo", "bar"), metrics.headers().get());
        assertTrue(metrics.compression().isPresent());
        assertEquals(NONE, metrics.compression().get());
        assertEquals(DurationConverter.parseDuration("10s"), metrics.timeout());
        assertTrue(metrics.protocol().isPresent());
        assertEquals(GRPC, metrics.protocol().get());
        assertTrue(metrics.keyCert().keys().isPresent());
        assertIterableEquals(List.of("keys"), metrics.keyCert().keys().get());
        assertTrue(metrics.keyCert().certs().isPresent());
        assertIterableEquals(List.of("certs"), metrics.keyCert().certs().get());
        assertTrue(metrics.trustCert().certs().isPresent());
        assertIterableEquals(List.of("certs"), metrics.trustCert().certs().get());
        assertTrue(metrics.tlsConfigurationName().isPresent());
        assertEquals("tls", metrics.tlsConfigurationName().get());
        assertFalse(metrics.proxyOptions().enabled());
        assertTrue(metrics.proxyOptions().username().isPresent());
        assertEquals("naruto", metrics.proxyOptions().username().get());
        assertTrue(metrics.proxyOptions().password().isPresent());
        assertEquals("hokage", metrics.proxyOptions().password().get());
        assertTrue(metrics.proxyOptions().host().isPresent());
        assertEquals("konoha", metrics.proxyOptions().host().get());
        assertTrue(metrics.proxyOptions().port().isPresent());
        assertEquals(9999, metrics.proxyOptions().port().getAsInt());
    }

    @Test
    void overrideTraces() {
        Map<String, String> baseExporterConfig = new HashMap<>();
        baseExporterConfig.put("quarkus.otel.exporter.otlp.headers", "foo,bar");
        baseExporterConfig.put("quarkus.otel.exporter.otlp.compression", "none");
        baseExporterConfig.put("quarkus.otel.exporter.otlp.key-cert.keys", "keys");
        baseExporterConfig.put("quarkus.otel.exporter.otlp.key-cert.certs", "certs");
        baseExporterConfig.put("quarkus.otel.exporter.otlp.trust-cert.certs", "certs");
        baseExporterConfig.put("quarkus.otel.exporter.otlp.tls-configuration-name", "tls");
        baseExporterConfig.put("quarkus.otel.exporter.otlp.proxy-options.username", "naruto");
        baseExporterConfig.put("quarkus.otel.exporter.otlp.proxy-options.password", "hokage");
        baseExporterConfig.put("quarkus.otel.exporter.otlp.proxy-options.host", "konoha");
        baseExporterConfig.put("quarkus.otel.exporter.otlp.proxy-options.port", "9999");

        Map<String, String> tracesExporterConfig = new HashMap<>();
        tracesExporterConfig.put("quarkus.otel.exporter.otlp.traces.endpoint", "traces");
        tracesExporterConfig.put("quarkus.otel.exporter.otlp.traces.protocol", HTTP_PROTOBUF);
        tracesExporterConfig.put("quarkus.otel.exporter.otlp.traces.timeout", "5s");
        tracesExporterConfig.put("quarkus.otel.exporter.otlp.traces.proxy-options.enabled", "true");
        tracesExporterConfig.put("quarkus.otel.exporter.otlp.traces.headers", "from,traces");
        tracesExporterConfig.put("quarkus.otel.exporter.otlp.traces.compression", "gzip");
        tracesExporterConfig.put("quarkus.otel.exporter.otlp.traces.key-cert.keys", "traces");
        tracesExporterConfig.put("quarkus.otel.exporter.otlp.traces.key-cert.certs", "traces");
        tracesExporterConfig.put("quarkus.otel.exporter.otlp.traces.trust-cert.certs", "traces");
        tracesExporterConfig.put("quarkus.otel.exporter.otlp.traces.tls-configuration-name", "traces");
        tracesExporterConfig.put("quarkus.otel.exporter.otlp.traces.proxy-options.username", "traces");
        tracesExporterConfig.put("quarkus.otel.exporter.otlp.traces.proxy-options.password", "traces");
        tracesExporterConfig.put("quarkus.otel.exporter.otlp.traces.proxy-options.host", "traces");
        tracesExporterConfig.put("quarkus.otel.exporter.otlp.traces.proxy-options.port", "6666");

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withCustomizers(builder -> new OtlpExporterConfigBuilder().configBuilder(builder))
                .withMapping(OtlpExporterRuntimeConfig.class)
                .withDefaultValues(baseExporterConfig)
                .withDefaultValues(tracesExporterConfig)
                .build();

        OtlpExporterRuntimeConfig mapping = config.getConfigMapping(OtlpExporterRuntimeConfig.class);

        OtlpExporterTracesConfig traces = mapping.traces();
        assertTrue(traces.endpoint().isPresent());
        assertEquals("traces", traces.endpoint().get());
        assertTrue(traces.headers().isPresent());
        assertIterableEquals(List.of("from", "traces"), traces.headers().get());
        assertTrue(traces.compression().isPresent());
        assertEquals(GZIP, traces.compression().get());
        assertEquals(DurationConverter.parseDuration("5s"), traces.timeout());
        assertTrue(traces.protocol().isPresent());
        assertEquals(HTTP_PROTOBUF, traces.protocol().get());
        assertTrue(traces.keyCert().keys().isPresent());
        assertIterableEquals(List.of("traces"), traces.keyCert().keys().get());
        assertTrue(traces.keyCert().certs().isPresent());
        assertIterableEquals(List.of("traces"), traces.keyCert().certs().get());
        assertTrue(traces.trustCert().certs().isPresent());
        assertIterableEquals(List.of("traces"), traces.trustCert().certs().get());
        assertTrue(traces.tlsConfigurationName().isPresent());
        assertEquals("traces", traces.tlsConfigurationName().get());
        assertTrue(traces.proxyOptions().enabled());
        assertTrue(traces.proxyOptions().username().isPresent());
        assertEquals("traces", traces.proxyOptions().username().get());
        assertTrue(traces.proxyOptions().password().isPresent());
        assertEquals("traces", traces.proxyOptions().password().get());
        assertTrue(traces.proxyOptions().host().isPresent());
        assertEquals("traces", traces.proxyOptions().host().get());
        assertTrue(traces.proxyOptions().port().isPresent());
        assertEquals(6666, traces.proxyOptions().port().getAsInt());
    }
}
