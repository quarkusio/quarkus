package io.quarkus.opentelemetry.runtime.exporter.otlp;

import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfig.DEFAULT_HTTP_BASE_URI;

import java.net.URI;

import io.opentelemetry.exporter.internal.EndpointUtil;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig;

/**
 * Resolves and validates the endpoints of the OTLP exporters.
 */
public final class OtlpExporterEndpoints {

    private OtlpExporterEndpoints() {
    }

    /**
     * The validated endpoint of the exporter of one signal, or {@code null} when the endpoint is empty.
     *
     * @param exporterRuntimeConfig the OTLP exporter configuration
     * @param signalConfig the configuration of the signal: {@code traces()}, {@code metrics()} or {@code logs()} of
     *        {@code exporterRuntimeConfig}
     * @throws IllegalArgumentException when the endpoint is not a valid URL
     */
    public static URI uri(OtlpExporterRuntimeConfig exporterRuntimeConfig, OtlpExporterConfig signalConfig) {
        String endpoint = resolveEndpoint(exporterRuntimeConfig, signalConfig);
        if (endpoint.isEmpty()) {
            return null;
        }
        return EndpointUtil.validateEndpoint(endpoint);
    }

    /**
     * The endpoint of the signal, else the global endpoint, else the default one. The default endpoint never wins over
     * another one.
     */
    static String resolveEndpoint(final OtlpExporterRuntimeConfig runtimeConfig, final OtlpExporterConfig signalConfig) {
        String endpoint = signalConfig.endpoint()
                .filter(OtlpExporterEndpoints::excludeDefaultEndpoint)
                .orElse(runtimeConfig.endpoint()
                        .filter(OtlpExporterEndpoints::excludeDefaultEndpoint)
                        .orElse(DEFAULT_HTTP_BASE_URI));
        return endpoint.trim();
    }

    private static boolean excludeDefaultEndpoint(String endpoint) {
        return !DEFAULT_HTTP_BASE_URI.equals(endpoint);
    }
}
