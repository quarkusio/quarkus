package io.quarkus.opentelemetry.runtime.exporter.otlp;

import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfig.Protocol.GRPC;
import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfig.Protocol.HTTP_PROTOBUF;
import static io.quarkus.opentelemetry.runtime.exporter.otlp.OTelExporterUtil.determineCompression;
import static io.quarkus.opentelemetry.runtime.exporter.otlp.OTelExporterUtil.populateTracingExportHttpHeaders;

import java.net.URI;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.otlp.internal.GrpcExporter;
import io.opentelemetry.exporter.otlp.internal.GrpcExporterBuilder;
import io.opentelemetry.exporter.otlp.internal.HttpExporter;
import io.opentelemetry.exporter.otlp.internal.HttpExporterBuilder;
import io.opentelemetry.sdk.common.InternalTelemetryVersion;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.common.internal.ComponentId;
import io.opentelemetry.sdk.common.internal.StandardComponentId;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.opentelemetry.runtime.OpenTelemetryUtil;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterTracesConfig;
import io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxGrpcSender;
import io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxHttpSender;
import io.quarkus.opentelemetry.runtime.exporter.otlp.tracing.VertxGrpcSpanExporter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.tracing.VertxHttpSpanExporter;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.config.Config;
import io.vertx.core.Vertx;

/**
 * Creates the default OTLP {@link SpanExporter} bean. Its endpoint is validated at startup.
 */
@SuppressWarnings("deprecation")
public class OtlpSpanExporterCreator implements BeanCreator<SpanExporter> {

    @Override
    public SpanExporter create(SyntheticCreationalContext<SpanExporter> context) {
        OpenTelemetryUtil.checkNotStaticInit(SpanExporter.class);
        Config config = Config.get();
        OtlpExporterRuntimeConfig exporterRuntimeConfig = config.getConfigMapping(OtlpExporterRuntimeConfig.class);
        URI baseUri = OtlpExporterEndpoints.uri(exporterRuntimeConfig, exporterRuntimeConfig.traces());
        if (config.getConfigMapping(OTelRuntimeConfig.class).sdkDisabled() || baseUri == null) {
            return SpanExporter.composite();
        }

        try {
            TlsConfigurationRegistry tlsConfigurationRegistry = context
                    .getInjectedReference(TlsConfigurationRegistry.class);

            return createSpanExporter(exporterRuntimeConfig, OTelExporterUtil.vertx(), baseUri,
                    tlsConfigurationRegistry);

        } catch (IllegalArgumentException iae) {
            throw new IllegalStateException("Unable to install OTLP Exporter", iae);
        }
    }

    private static SpanExporter createSpanExporter(OtlpExporterRuntimeConfig exporterRuntimeConfig,
            Vertx vertx,
            URI baseUri,
            TlsConfigurationRegistry tlsConfigurationRegistry) {
        OtlpExporterTracesConfig tracesConfig = exporterRuntimeConfig.traces();
        if (tracesConfig.protocol().isEmpty()) {
            throw new IllegalStateException("No OTLP protocol specified. " +
                    "Please check `quarkus.otel.exporter.otlp.traces.protocol` property");
        }

        String protocol = tracesConfig.protocol().get();
        if (GRPC.equals(protocol)) {
            return createOtlpGrpcSpanExporter(exporterRuntimeConfig, vertx, baseUri,
                    tlsConfigurationRegistry);
        } else if (HTTP_PROTOBUF.equals(protocol)) {
            return createHttpSpanExporter(exporterRuntimeConfig, vertx, baseUri, protocol,
                    tlsConfigurationRegistry);
        }

        throw new IllegalArgumentException(String.format("Unsupported OTLP protocol %s specified. " +
                "Please check `quarkus.otel.exporter.otlp.traces.protocol` property", protocol));
    }

    private static SpanExporter createOtlpGrpcSpanExporter(OtlpExporterRuntimeConfig exporterRuntimeConfig,
            Vertx vertx, final URI baseUri,
            TlsConfigurationRegistry tlsConfigurationRegistry) {

        OtlpExporterTracesConfig tracesConfig = exporterRuntimeConfig.traces();
        MemoryMode memoryMode = exporterRuntimeConfig.memoryMode();

        return new VertxGrpcSpanExporter(new GrpcExporter(
                new VertxGrpcSender(
                        baseUri,
                        VertxGrpcSender.GRPC_TRACE_SERVICE_NAME,
                        determineCompression(tracesConfig),
                        tracesConfig.timeout(),
                        populateTracingExportHttpHeaders(tracesConfig),
                        new HttpClientOptionsConsumer(tracesConfig, baseUri, tlsConfigurationRegistry),
                        vertx),
                InternalTelemetryVersion.LATEST,
                ComponentId.generateLazy(StandardComponentId.ExporterType.OTLP_GRPC_SPAN_EXPORTER), // use the same as OTel does
                MeterProvider::noop,
                baseUri,
                GrpcExporterBuilder.DEFAULT_MAX_REQUEST_MESSAGE_SIZE),
                memoryMode);
    }

    private static SpanExporter createHttpSpanExporter(OtlpExporterRuntimeConfig exporterRuntimeConfig, Vertx vertx,
            URI baseUri, String protocol,
            TlsConfigurationRegistry tlsConfigurationRegistry) {

        OtlpExporterTracesConfig tracesConfig = exporterRuntimeConfig.traces();
        MemoryMode memoryMode = exporterRuntimeConfig.memoryMode();

        boolean exportAsJson = false; //TODO: this will be enhanced in the future

        return new VertxHttpSpanExporter(new HttpExporter(
                ComponentId.generateLazy(StandardComponentId.ExporterType.OTLP_HTTP_SPAN_EXPORTER),
                new VertxHttpSender(
                        baseUri,
                        VertxHttpSender.TRACES_PATH,
                        determineCompression(tracesConfig),
                        tracesConfig.timeout(),
                        populateTracingExportHttpHeaders(tracesConfig),
                        exportAsJson ? "application/json" : "application/x-protobuf",
                        new HttpClientOptionsConsumer(tracesConfig, baseUri, tlsConfigurationRegistry),
                        vertx),
                MeterProvider::noop,
                InternalTelemetryVersion.LATEST,
                baseUri,
                false,
                HttpExporterBuilder.DEFAULT_MAX_REQUEST_BODY_SIZE),
                memoryMode);
    }
}
