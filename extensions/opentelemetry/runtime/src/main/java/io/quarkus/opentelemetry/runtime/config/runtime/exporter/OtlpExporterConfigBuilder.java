package io.quarkus.opentelemetry.runtime.config.runtime.exporter;

import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfig.DEFAULT_GRPC_BASE_URI;
import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfig.Protocol.GRPC;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.FallbackConfigSourceInterceptor;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Adds fallbacks to {@link OtlpExporterRuntimeConfig#traces()} and {@link OtlpExporterRuntimeConfig#metrics()} from
 * the base configuration {@link OtlpExporterRuntimeConfig}. The goal is to fallback specific properties from either
 * <code>tracer</code> or <code>metrics</code> to their parent configuration. For instance, if there is no value for
 * <code>quarkus.otel.exporter.otlp.traces.endpoint</code> it fallbacks to
 * <code>quarkus.otel.exporter.otlp.endpoint</code>.
 * <p>
 * Defaults are set using the {@link SmallRyeConfigBuilder#withDefaultValue(String, String)}, instead of
 * {@link io.smallrye.config.WithDefault} because the mapping {@link OtlpExporterConfig} is shared between base
 * (unnamed), <code>traces</code>, and <code>metrics</code>, which means that every path would always have a default,
 * and it won't be possible to fallback from the specific configuration to the base configuration.
 * <p>
 * This builder is only set to customize the runtime configuration since the mapping {@link OtlpExporterRuntimeConfig}
 * is only for runtime. The builder executes with a very high priority to ensure that it sets the default
 * configuration early in the config setup to allow other configurations to override it (like Dev Services).
 */
public class OtlpExporterConfigBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
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

        //Logs
        fallbacks.put("quarkus.otel.exporter.otlp.logs.endpoint", "quarkus.otel.exporter.otlp.endpoint");
        fallbacks.put("quarkus.otel.exporter.otlp.logs.headers", "quarkus.otel.exporter.otlp.headers");
        fallbacks.put("quarkus.otel.exporter.otlp.logs.compression", "quarkus.otel.exporter.otlp.compression");
        fallbacks.put("quarkus.otel.exporter.otlp.logs.timeout", "quarkus.otel.exporter.otlp.timeout");
        fallbacks.put("quarkus.otel.exporter.otlp.logs.protocol", "quarkus.otel.exporter.otlp.protocol");
        fallbacks.put("quarkus.otel.exporter.otlp.logs.key-cert.keys", "quarkus.otel.exporter.otlp.key-cert.keys");
        fallbacks.put("quarkus.otel.exporter.otlp.logs.key-cert.certs", "quarkus.otel.exporter.otlp.key-cert.certs");
        fallbacks.put("quarkus.otel.exporter.otlp.logs.trust-cert.certs", "quarkus.otel.exporter.otlp.trust-cert.certs");
        fallbacks.put("quarkus.otel.exporter.otlp.logs.tls-configuration-name",
                "quarkus.otel.exporter.otlp.tls-configuration-name");
        fallbacks.put("quarkus.otel.exporter.otlp.logs.proxy-options.enabled",
                "quarkus.otel.exporter.otlp.proxy-options.enabled");
        fallbacks.put("quarkus.otel.exporter.otlp.logs.proxy-options.username",
                "quarkus.otel.exporter.otlp.proxy-options.username");
        fallbacks.put("quarkus.otel.exporter.otlp.logs.proxy-options.password",
                "quarkus.otel.exporter.otlp.proxy-options.password");
        fallbacks.put("quarkus.otel.exporter.otlp.logs.proxy-options.host", "quarkus.otel.exporter.otlp.proxy-options.host");
        fallbacks.put("quarkus.otel.exporter.otlp.logs.proxy-options.port", "quarkus.otel.exporter.otlp.proxy-options.port");

        return builder.withInterceptors(new FallbackConfigSourceInterceptor(fallbacks));
    }

    @Override
    public int priority() {
        return Integer.MIN_VALUE;
    }
}
