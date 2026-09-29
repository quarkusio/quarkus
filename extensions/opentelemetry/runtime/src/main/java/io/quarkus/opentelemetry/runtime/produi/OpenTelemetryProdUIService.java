package io.quarkus.opentelemetry.runtime.produi;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.smallrye.common.annotation.NonBlocking;

/**
 * Read-only Prod UI view of the OpenTelemetry configuration: whether the SDK and
 * tracing are enabled, the configured span exporter(s), sampler and its argument,
 * the OTLP exporter endpoint/protocol/compression/timeout, and a derived span
 * export status.
 * <p>
 * There is no Dev UI data page to reuse (the OpenTelemetry Dev UI ships only i18n
 * bundles), so a bespoke read-only component + service is provided. It reads only
 * configuration and mutates nothing. Secret-bearing exporter settings are never
 * exposed: OTLP {@code headers} (which commonly carry authentication tokens), the
 * PEM key/cert and trust-cert configuration are deliberately omitted, and any
 * credentials embedded in the endpoint URL are stripped. Only whether headers are
 * present is reported, not their values.
 */
@ApplicationScoped
public class OpenTelemetryProdUIService {

    @Inject
    OTelBuildConfig buildConfig;

    @Inject
    OTelRuntimeConfig runtimeConfig;

    @Inject
    OtlpExporterRuntimeConfig otlpConfig;

    @NonBlocking
    @JsonRpcUsage({ Usage.PROD_UI })
    @JsonRpcDescription("Get a read-only view of the OpenTelemetry exporter/sampler configuration and span export status (no secrets)")
    public OTelInfo getInfo() {
        boolean sdkEnabled = buildConfig.enabled();
        boolean sdkDisabled = runtimeConfig.sdkDisabled();
        boolean tracesEnabled = buildConfig.traces().enabled().orElse(Boolean.TRUE);
        List<String> exporters = buildConfig.traces().exporter();
        boolean exportsToNone = exporters.size() == 1 && "none".equalsIgnoreCase(exporters.get(0));

        boolean spanExportEnabled = sdkEnabled && !sdkDisabled && tracesEnabled && !exportsToNone;

        var tracesExporter = otlpConfig.traces();
        String endpoint = sanitize(tracesExporter.endpoint().orElse(otlpConfig.endpoint().orElse(null)));
        String protocol = tracesExporter.protocol().orElse(otlpConfig.protocol().orElse(null));
        String compression = tracesExporter.compression()
                .map(c -> c.getValue())
                .orElse(otlpConfig.compression().map(c -> c.getValue()).orElse(null));
        long timeoutMs = tracesExporter.timeout().toMillis();
        boolean headersConfigured = tracesExporter.headers().map(h -> !h.isEmpty()).orElse(Boolean.FALSE)
                || otlpConfig.headers().map(h -> !h.isEmpty()).orElse(Boolean.FALSE);

        return new OTelInfo(
                sdkEnabled,
                sdkDisabled,
                spanExportEnabled,
                runtimeConfig.serviceName().orElse(null),
                buildConfig.simple(),
                buildConfig.propagators(),
                tracesEnabled,
                exporters,
                buildConfig.traces().sampler(),
                runtimeConfig.traces().samplerArg().orElse(null),
                runtimeConfig.traces().suppressNonApplicationUris(),
                runtimeConfig.traces().includeStaticResources(),
                endpoint,
                protocol,
                compression,
                timeoutMs,
                headersConfigured);
    }

    /**
     * Strip any {@code user:password@} userinfo from an endpoint URL so embedded
     * credentials are never exposed.
     */
    private static String sanitize(String endpoint) {
        if (endpoint == null) {
            return null;
        }
        return endpoint.replaceFirst("://[^/@]*@", "://");
    }

    public record OTelInfo(boolean sdkEnabled, boolean sdkDisabled, boolean spanExportEnabled, String serviceName,
            boolean simpleProcessor, List<String> propagators, boolean tracesEnabled, List<String> exporters,
            String sampler, String samplerArg, boolean suppressNonApplicationUris, boolean includeStaticResources,
            String exporterEndpoint, String exporterProtocol, String exporterCompression, long exporterTimeoutMs,
            boolean headersConfigured) {
    }
}
