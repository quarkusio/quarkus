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
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.opentelemetry.runtime.OpenTelemetryUtil;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterLogsConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig;
import io.quarkus.opentelemetry.runtime.exporter.otlp.logs.NoopLogRecordExporter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.logs.VertxGrpcLogRecordExporter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.logs.VertxHttpLogRecordExporter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxGrpcSender;
import io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxHttpSender;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.config.Config;

/**
 * Creates the default OTLP {@link LogRecordExporter} bean. Its endpoint is validated at startup.
 */
@SuppressWarnings("deprecation")
public class OtlpLogRecordExporterCreator implements BeanCreator<LogRecordExporter> {

    @Override
    public LogRecordExporter create(SyntheticCreationalContext<LogRecordExporter> context) {
        OpenTelemetryUtil.checkNotStaticInit(LogRecordExporter.class);
        Config config = Config.get();
        OtlpExporterRuntimeConfig exporterRuntimeConfig = config.getConfigMapping(OtlpExporterRuntimeConfig.class);
        URI baseUri = OtlpExporterEndpoints.uri(exporterRuntimeConfig, exporterRuntimeConfig.logs());
        if (config.getConfigMapping(OTelRuntimeConfig.class).sdkDisabled() || baseUri == null) {
            return NoopLogRecordExporter.INSTANCE;
        }

        MemoryMode memoryMode = exporterRuntimeConfig.memoryMode();
        LogRecordExporter logRecordExporter;

        try {
            TlsConfigurationRegistry tlsConfigurationRegistry = context
                    .getInjectedReference(TlsConfigurationRegistry.class);
            OtlpExporterLogsConfig logsConfig = exporterRuntimeConfig.logs();
            if (logsConfig.protocol().isEmpty()) {
                throw new IllegalStateException("No OTLP protocol specified. " +
                        "Please check `quarkus.otel.exporter.otlp.logs.protocol` property");
            }

            String protocol = logsConfig.protocol().get();
            if (GRPC.equals(protocol)) {
                logRecordExporter = new VertxGrpcLogRecordExporter(
                        new GrpcExporter(
                                new VertxGrpcSender(
                                        baseUri,
                                        VertxGrpcSender.GRPC_LOG_SERVICE_NAME,
                                        determineCompression(logsConfig),
                                        logsConfig.timeout(),
                                        populateTracingExportHttpHeaders(logsConfig),
                                        new HttpClientOptionsConsumer(logsConfig, baseUri, tlsConfigurationRegistry),
                                        OTelExporterUtil.vertx()),
                                InternalTelemetryVersion.LATEST,
                                ComponentId.generateLazy(
                                        StandardComponentId.ExporterType.OTLP_GRPC_LOG_EXPORTER), // use the same as OTel does
                                MeterProvider::noop,
                                baseUri,
                                GrpcExporterBuilder.DEFAULT_MAX_REQUEST_MESSAGE_SIZE),
                        memoryMode);
            } else if (HTTP_PROTOBUF.equals(protocol)) {
                boolean exportAsJson = false; //TODO: this will be enhanced in the future
                logRecordExporter = new VertxHttpLogRecordExporter(
                        new HttpExporter(
                                ComponentId.generateLazy(
                                        StandardComponentId.ExporterType.OTLP_HTTP_LOG_EXPORTER),
                                new VertxHttpSender(
                                        baseUri,
                                        VertxHttpSender.LOGS_PATH,
                                        determineCompression(logsConfig),
                                        logsConfig.timeout(),
                                        populateTracingExportHttpHeaders(logsConfig),
                                        exportAsJson ? "application/json" : "application/x-protobuf",
                                        new HttpClientOptionsConsumer(logsConfig, baseUri, tlsConfigurationRegistry),
                                        OTelExporterUtil.vertx()),
                                MeterProvider::noop,
                                InternalTelemetryVersion.LATEST,
                                baseUri,
                                false,
                                HttpExporterBuilder.DEFAULT_MAX_REQUEST_BODY_SIZE),
                        memoryMode);
            } else {
                throw new IllegalArgumentException(String.format("Unsupported OTLP protocol %s specified. " +
                        "Please check `quarkus.otel.exporter.otlp.logs.protocol` property", protocol));
            }

        } catch (IllegalArgumentException iae) {
            throw new IllegalStateException("Unable to install OTLP Exporter", iae);
        }
        return logRecordExporter;
    }
}
