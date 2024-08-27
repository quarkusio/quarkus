package io.quarkus.opentelemetry.runtime.config;

import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfig.DEFAULT_GRPC_BASE_URI;
import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfig.Protocol.GRPC;

import java.util.HashMap;
import java.util.Map;

import io.smallrye.config.FallbackConfigSourceInterceptor;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

public class OpenTelemetryConfigBuilderCustomizer implements SmallRyeConfigBuilderCustomizer {
    @Override
    public void configBuilder(final SmallRyeConfigBuilder builder) {
        // Main defaults
        builder.withDefaultValue("quarkus.otel.exporter.otlp.endpoint", DEFAULT_GRPC_BASE_URI);
        builder.withDefaultValue("quarkus.otel.exporter.otlp.protocol", GRPC);
        builder.withDefaultValue("quarkus.otel.exporter.otlp.timeout", "10s");
        builder.withDefaultValue("quarkus.otel.exporter.otlp.proxy-options.enabled", "false");

        Map<String, String> fallbacks = new HashMap<>(30);
        // Traces
        fallbacks.put("quarkus.otel.exporter.otlp.traces.endpoint", "quarkus.otel.exporter.otlp.endpoint");
        fallbacks.put("quarkus.otel.exporter.otlp.traces.headers", "quarkus.otel.exporter.otlp.headers");
        fallbacks.put("quarkus.otel.exporter.otlp.traces.compression", "quarkus.otel.exporter.otlp.compression");
        fallbacks.put("quarkus.otel.exporter.otlp.traces.timeout", "quarkus.otel.exporter.otlp.timeout");
        fallbacks.put("quarkus.otel.exporter.otlp.traces.protocol", "quarkus.otel.exporter.otlp.protocol");
        fallbacks.put("quarkus.otel.exporter.otlp.traces.key-cert.keys", "quarkus.otel.exporter.otlp.key-cert.keys");
        fallbacks.put("quarkus.otel.exporter.otlp.traces.key-cert.certs", "quarkus.otel.exporter.otlp.key-cert.certs");
        fallbacks.put("quarkus.otel.exporter.otlp.traces.trust-cert.certs", "quarkus.otel.exporter.otlp.trust-cert.certs");
        fallbacks.put("quarkus.otel.exporter.otlp.traces.tls-configuration-name",
                "quarkus.otel.exporter.otlp.tls-configuration-name");
        fallbacks.put("quarkus.otel.exporter.otlp.traces.proxy-options.enabled",
                "quarkus.otel.exporter.otlp.proxy-options.enabled");
        fallbacks.put("quarkus.otel.exporter.otlp.traces.proxy-options.username",
                "quarkus.otel.exporter.otlp.proxy-options.username");
        fallbacks.put("quarkus.otel.exporter.otlp.traces.proxy-options.password",
                "quarkus.otel.exporter.otlp.proxy-options.password");
        fallbacks.put("quarkus.otel.exporter.otlp.traces.proxy-options.host", "quarkus.otel.exporter.otlp.proxy-options.host");
        fallbacks.put("quarkus.otel.exporter.otlp.traces.proxy-options.port", "quarkus.otel.exporter.otlp.proxy-options.port");

        // Metrics
        fallbacks.put("quarkus.otel.exporter.otlp.metrics.endpoint", "quarkus.otel.exporter.otlp.endpoint");
        fallbacks.put("quarkus.otel.exporter.otlp.metrics.headers", "quarkus.otel.exporter.otlp.headers");
        fallbacks.put("quarkus.otel.exporter.otlp.metrics.compression", "quarkus.otel.exporter.otlp.compression");
        fallbacks.put("quarkus.otel.exporter.otlp.metrics.timeout", "quarkus.otel.exporter.otlp.timeout");
        fallbacks.put("quarkus.otel.exporter.otlp.metrics.protocol", "quarkus.otel.exporter.otlp.protocol");
        fallbacks.put("quarkus.otel.exporter.otlp.metrics.key-cert.keys", "quarkus.otel.exporter.otlp.key-cert.keys");
        fallbacks.put("quarkus.otel.exporter.otlp.metrics.key-cert.certs", "quarkus.otel.exporter.otlp.key-cert.certs");
        fallbacks.put("quarkus.otel.exporter.otlp.metrics.trust-cert.certs", "quarkus.otel.exporter.otlp.trust-cert.certs");
        fallbacks.put("quarkus.otel.exporter.otlp.metrics.tls-configuration-name",
                "quarkus.otel.exporter.otlp.tls-configuration-name");
        fallbacks.put("quarkus.otel.exporter.otlp.metrics.proxy-options.enabled",
                "quarkus.otel.exporter.otlp.proxy-options.enabled");
        fallbacks.put("quarkus.otel.exporter.otlp.metrics.proxy-options.username",
                "quarkus.otel.exporter.otlp.proxy-options.username");
        fallbacks.put("quarkus.otel.exporter.otlp.metrics.proxy-options.password",
                "quarkus.otel.exporter.otlp.proxy-options.password");
        fallbacks.put("quarkus.otel.exporter.otlp.metrics.proxy-options.host", "quarkus.otel.exporter.otlp.proxy-options.host");
        fallbacks.put("quarkus.otel.exporter.otlp.metrics.proxy-options.port", "quarkus.otel.exporter.otlp.proxy-options.port");

        builder.withInterceptors(new FallbackConfigSourceInterceptor(fallbacks));
    }
}
