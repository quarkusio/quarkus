package io.quarkus.opentelemetry.exporter.jaeger.runtime;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.CDI;

import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessorBuilder;
import io.quarkus.opentelemetry.runtime.config.OtelRuntimeConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class JaegerRecorder {

    static String resolveEndpoint(JaegerExporterRuntimeConfig runtimeConfig) {
        String endpoint = runtimeConfig.traces().legacyEndpoint()
                .orElse(runtimeConfig.endpoint()
                        .map(s -> s + runtimeConfig.traces().endpoint().orElse(""))
                        .orElse(""));
        return endpoint;
    }

    public void installBatchSpanProcessorForJaeger(
            OtelRuntimeConfig otelRuntimeConfig,
            JaegerExporterRuntimeConfig runtimeConfig,
            LaunchMode launchMode) {

        String endpoint = resolveEndpoint(runtimeConfig).trim();

        if (launchMode == LaunchMode.DEVELOPMENT && !endpoint.isEmpty()) {
            // Default the endpoint for development only
            endpoint = "http://localhost:14250";
        }

        // Only create the JaegerGrpcSpanExporter if an endpoint was set in runtime config
        if (endpoint.length() > 0) {
            try {
                JaegerGrpcSpanExporter jaegerSpanExporter = JaegerGrpcSpanExporter.builder()
                        .setEndpoint(endpoint)
                        .setTimeout(runtimeConfig.timeout())
                        .build();

                // Create BatchSpanProcessor for Jaeger and install into LateBoundBatchSpanProcessor
                LateBoundBatchSpanProcessor delayedProcessor = CDI.current()
                        .select(LateBoundBatchSpanProcessor.class, Any.Literal.INSTANCE).get();

                BatchSpanProcessorBuilder processorBuilder = BatchSpanProcessor.builder(jaegerSpanExporter);

                processorBuilder.setScheduleDelay(otelRuntimeConfig.bsp().scheduleDelay());
                processorBuilder.setMaxQueueSize(otelRuntimeConfig.bsp().maxQueueSize());
                processorBuilder.setMaxExportBatchSize(otelRuntimeConfig.bsp().maxExportBatchSize());
                processorBuilder.setExporterTimeout(otelRuntimeConfig.bsp().exportTimeout());
                //                processorBuilder.setMeterProvider() // TODO add meter provider to span processor.

                delayedProcessor.setBatchSpanProcessorDelegate(processorBuilder.build());
            } catch (IllegalArgumentException iae) {
                throw new IllegalStateException("Unable to install Jaeger Exporter", iae);
            }
        }
    }
}
