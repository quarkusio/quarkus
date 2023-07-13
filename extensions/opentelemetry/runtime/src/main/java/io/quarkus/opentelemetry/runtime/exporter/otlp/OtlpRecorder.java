package io.quarkus.opentelemetry.runtime.exporter.otlp;

import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig.DEFAULT_GRPC_BASE_URI;
import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterTracesConfig.Protocol.HTTP_PROTOBUF;

import java.util.List;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.CDI;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessorBuilder;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.Recorder;

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
            LaunchMode launchMode) {

        if (otelRuntimeConfig.sdkDisabled()) {
            return;
        }
        String endpoint = resolveEndpoint(exporterRuntimeConfig).trim();

        // Only create the OtlpGrpcSpanExporter if an endpoint was set in runtime config
        if (endpoint.length() > 0) {
            try {
                // Load span exporter if provided by user
                SpanExporter spanExporter = CDI.current()
                        .select(SpanExporter.class, Any.Literal.INSTANCE).stream().findFirst().orElse(null);
                // CDI exporter was already added to a processor by OTEL
                if (spanExporter == null) {
                    spanExporter = createOtlpGrpcSpanExporter(exporterRuntimeConfig, endpoint);

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

    private OtlpGrpcSpanExporter createOtlpGrpcSpanExporter(OtlpExporterRuntimeConfig exporterRuntimeConfig, String endpoint) {
        OtlpGrpcSpanExporterBuilder exporterBuilder = OtlpGrpcSpanExporter.builder()
                .setEndpoint(endpoint)
                .setTimeout(exporterRuntimeConfig.traces().timeout());

        // FIXME TLS Support. Was not available before but will be available soon.
        // exporterRuntimeConfig.traces.certificate.ifPresent(exporterBuilder::setTrustedCertificates);
        // exporterRuntimeConfig.client.ifPresent(exporterBuilder::setClientTls);

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
                    exporterBuilder.addHeader(key, value);
                }
            }
        }

        if (exporterRuntimeConfig.traces().compression().isPresent()) {
            exporterBuilder.setCompression(exporterRuntimeConfig.traces().compression().get().getValue());
        }

        if (exporterRuntimeConfig.traces().protocol().isPresent()) {
            if (!exporterRuntimeConfig.traces().protocol().get().equals(HTTP_PROTOBUF)) {
                throw new IllegalStateException("Only the GRPC Exporter is currently supported. " +
                        "Please check `quarkus.otel.exporter.otlp.traces.protocol` property");
            }
        }

        return exporterBuilder.build();
    }
}
