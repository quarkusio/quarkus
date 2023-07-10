package io.quarkus.opentelemetry.runtime.exporter.otlp;

import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig.DEFAULT_GRPC_BASE_URI;
import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterTracesConfig.Protocol.HTTP_PROTOBUF;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.CDI;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.internal.ExporterBuilderUtil;
import io.opentelemetry.exporter.internal.otlp.OtlpUserAgent;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessorBuilder;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.CompressionType;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Vertx;

@Recorder
public class OtlpRecorder {

    static String resolveEndpoint(final OtlpExporterRuntimeConfig runtimeConfig) {
        String endpoint = runtimeConfig.traces().legacyEndpoint()
                .filter(OtlpRecorder::excludeDefaultEndpoint)
                .orElse(runtimeConfig.traces().endpoint()
                        .filter(OtlpRecorder::excludeDefaultEndpoint)
                        .orElse(runtimeConfig.endpoint()
                                .filter(OtlpRecorder::excludeDefaultEndpoint)
                                .orElse(DEFAULT_GRPC_BASE_URI)));
        return endpoint.trim();
    }

    private static boolean excludeDefaultEndpoint(String endpoint) {
        return !DEFAULT_GRPC_BASE_URI.equals(endpoint);
    }

    public void installBatchSpanProcessorForOtlp(
            OTelRuntimeConfig otelRuntimeConfig,
            OtlpExporterRuntimeConfig exporterRuntimeConfig,
            RuntimeValue<Vertx> vertx,
            LaunchMode launchMode) {

        if (otelRuntimeConfig.sdkDisabled()) {
            return;
        }
        String grpcBaseUri = resolveEndpoint(exporterRuntimeConfig).trim();

        // Only create the OtlpGrpcSpanExporter if an endpoint was set in runtime config
        if (grpcBaseUri.length() > 0) {
            try {
                // Load span exporter if provided by user
                SpanExporter spanExporter = CDI.current()
                        .select(SpanExporter.class, Any.Literal.INSTANCE).stream().findFirst().orElse(null);
                // CDI exporter was already added to a processor by OTEL
                if (spanExporter == null) {
                    spanExporter = createOtlpGrpcSpanExporter(exporterRuntimeConfig, grpcBaseUri, vertx.getValue());

                    // Create BatchSpanProcessor for OTLP and install into LateBoundBatchSpanProcessor
                    LateBoundBatchSpanProcessor delayedProcessor = CDI.current()
                            .select(LateBoundBatchSpanProcessor.class, Any.Literal.INSTANCE).get();

                    BatchSpanProcessorBuilder processorBuilder = BatchSpanProcessor.builder(spanExporter);

                    processorBuilder.setScheduleDelay(otelRuntimeConfig.bsp().scheduleDelay());
                    processorBuilder.setMaxQueueSize(otelRuntimeConfig.bsp().maxQueueSize());
                    processorBuilder.setMaxExportBatchSize(otelRuntimeConfig.bsp().maxExportBatchSize());
                    processorBuilder.setExporterTimeout(otelRuntimeConfig.bsp().exportTimeout());
                    // processorBuilder.setMeterProvider() // TODO add meter provider to span processor.

                    delayedProcessor.setBatchSpanProcessorDelegate(processorBuilder.build());
                }
            } catch (IllegalArgumentException iae) {
                throw new IllegalStateException("Unable to install OTLP Exporter", iae);
            }
        }
    }

    private SpanExporter createOtlpGrpcSpanExporter(OtlpExporterRuntimeConfig exporterRuntimeConfig, String endpoint,
            Vertx vertx) {

        if (exporterRuntimeConfig.traces().protocol().isPresent()) {
            if (!exporterRuntimeConfig.traces().protocol().get().equals(HTTP_PROTOBUF)) {
                throw new IllegalStateException("Only the GRPC Exporter is currently supported. " +
                        "Please check `quarkus.otel.exporter.otlp.traces.protocol` property");
            }
        }

        boolean compressionEnabled = false;
        if (exporterRuntimeConfig.traces().compression().isPresent()) {
            compressionEnabled = (exporterRuntimeConfig.traces().compression().get() == CompressionType.GZIP);
        }

        // FIXME TLS Support. Was not available before but will be available soon.
        // exporterRuntimeConfig.traces.certificate.ifPresent(exporterBuilder::setTrustedCertificates);
        // exporterRuntimeConfig.client.ifPresent(exporterBuilder::setClientTls);

        Map<String, String> headersMap = new HashMap<>();
        OtlpUserAgent.addUserAgentHeader(headersMap::put);
        if (exporterRuntimeConfig.traces().headers().isPresent()) {
            List<String> headers = exporterRuntimeConfig.traces().headers().get();
            if (!headers.isEmpty()) {
                for (String header : headers) {
                    if (header.isEmpty()) {
                        continue;
                    }
                    String[] parts = header.split("=", 2);
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    headersMap.put(key, value);
                }
            }
        }

        return new VertxGrpcExporter(
                "otlp", // use the same as OTel does
                "span", // use the same as OTel does
                MeterProvider::noop,
                ExporterBuilderUtil.validateEndpoint(endpoint),
                compressionEnabled,
                exporterRuntimeConfig.traces().timeout(),
                headersMap,
                vertx);

    }
}
