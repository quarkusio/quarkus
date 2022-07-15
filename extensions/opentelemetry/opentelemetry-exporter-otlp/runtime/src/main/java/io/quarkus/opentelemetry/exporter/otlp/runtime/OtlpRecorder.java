package io.quarkus.opentelemetry.exporter.otlp.runtime;

import static io.quarkus.opentelemetry.exporter.otlp.runtime.OtlpExporterTracesConfig.Protocol.HTTP_PROTOBUF;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.CDI;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessorBuilder;
import io.quarkus.opentelemetry.runtime.config.OtelRuntimeConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class OtlpRecorder {

    static String resolveEndpoint(OtlpExporterRuntimeConfig runtimeConfig) {
        String endpoint = runtimeConfig.traces().legacyEndpoint()
                .orElse(runtimeConfig.endpoint()
                        .map(s -> s + runtimeConfig.traces().endpoint().orElse(""))
                        .orElse(""));
        return endpoint;
    }

    public void installBatchSpanProcessorForOtlp(
            OtelRuntimeConfig otelRuntimeConfig,
            OtlpExporterRuntimeConfig exporterRuntimeConfig,
            LaunchMode launchMode) {

        String endpoint = resolveEndpoint(exporterRuntimeConfig).trim();

        if (launchMode == LaunchMode.DEVELOPMENT && endpoint.isEmpty()) {
            // Default the endpoint for development only
            endpoint = "http://localhost:4317";
        }

        // Only create the OtlpGrpcSpanExporter if an endpoint was set in runtime config
        if (endpoint.length() > 0) {
            try {
                OtlpGrpcSpanExporterBuilder exporterBuilder = OtlpGrpcSpanExporter.builder()
                        .setEndpoint(endpoint)
                        .setTimeout(exporterRuntimeConfig.traces().timeout());

                exporterRuntimeConfig.traces().certificate().ifPresent(exporterBuilder::setTrustedCertificates);

                //                runtimeConfig.client().ifPresent(exporterBuilder::setClientTls); // FIXME TLS Support

                exporterRuntimeConfig.traces().headers().forEach(exporterBuilder::addHeader);

                exporterRuntimeConfig.traces().compression()
                        .ifPresent(compression -> exporterBuilder.setCompression(compression.getValue()));

                if (!exporterRuntimeConfig.traces().protocol().orElse("").equals(HTTP_PROTOBUF)) {
                    throw new IllegalStateException("Only the GRPC Exporter is currently supported. " +
                            "Please check `otel.exporter.otlp.traces.protocol` property");
                }

                OtlpGrpcSpanExporter otlpSpanExporter = exporterBuilder.build();

                // Create BatchSpanProcessor for OTLP and install into LateBoundBatchSpanProcessor
                LateBoundBatchSpanProcessor delayedProcessor = CDI.current()
                        .select(LateBoundBatchSpanProcessor.class, Any.Literal.INSTANCE).get();

                BatchSpanProcessorBuilder processorBuilder = BatchSpanProcessor.builder(otlpSpanExporter);

                processorBuilder.setScheduleDelay(otelRuntimeConfig.bsp().scheduleDelay());
                processorBuilder.setMaxQueueSize(otelRuntimeConfig.bsp().maxQueueSize());
                processorBuilder.setMaxExportBatchSize(otelRuntimeConfig.bsp().maxExportBatchSize());
                processorBuilder.setExporterTimeout(otelRuntimeConfig.bsp().exportTimeout());
                //                processorBuilder.setMeterProvider() // TODO add meter provider to span processor.

                delayedProcessor.setBatchSpanProcessorDelegate(processorBuilder.build());
            } catch (IllegalArgumentException iae) {
                throw new IllegalStateException("Unable to install OTLP Exporter", iae);
            }
        }
    }
}
