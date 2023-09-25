package io.quarkus.opentelemetry.runtime.exporter.otlp;

import static io.quarkus.opentelemetry.runtime.OpenTelemetryUtil.convertKeyValueListToMap;
import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig.DEFAULT_GRPC_BASE_URI;
import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterTracesConfig.Protocol.GRPC;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.CDI;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessorBuilder;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.CompressionType;
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
        exporterRuntimeConfig.traces().headers().ifPresent(new Consumer<List<String>>() {
            @Override
            public void accept(final List<String> headers) {
                for (Map.Entry<String, String> entry : convertKeyValueListToMap(headers).entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    exporterBuilder.addHeader(key, value);
                }
            }
        });
        exporterRuntimeConfig.traces().compression()
                .ifPresent(new Consumer<CompressionType>() {
                    @Override
                    public void accept(CompressionType compression) {
                        exporterBuilder.setCompression(compression.getValue());
                    }
                });

        if (!exporterRuntimeConfig.traces().protocol().orElse("").equals(GRPC)) {
            throw new IllegalStateException("Only the `grpc` protocol is currently supported. " +
                    "`http/protobuf` is available after Quarkus 3.3. " +
                    "Please check `otel.exporter.otlp.traces.protocol` property");
        }
        return exporterBuilder.build();
    }
}
