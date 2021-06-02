package io.quarkus.opentelemetry.exporter.jaeger.runtime;

import java.util.Optional;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.CDI;

import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class JaegerRecorder {
    public void installBatchSpanProcessorForJaeger(JaegerExporterConfig.JaegerExporterRuntimeConfig runtimeConfig,
            LaunchMode launchMode) {

        if (launchMode == LaunchMode.DEVELOPMENT && !runtimeConfig.endpoint.isPresent()) {
            // Default the endpoint for development only
            runtimeConfig.endpoint = Optional.of("http://localhost:14250");
        }

        // Only create the JaegerGrpcSpanExporter if an endpoint was set in runtime config
        if (runtimeConfig.endpoint.isPresent() && runtimeConfig.endpoint.get().trim().length() > 0) {
            try {
                JaegerGrpcSpanExporter jaegerSpanExporter = JaegerGrpcSpanExporter.builder()
                        .setEndpoint(runtimeConfig.endpoint.get())
                        .setTimeout(runtimeConfig.exportTimeout)
                        .build();

                // Create BatchSpanProcessor for Jaeger and install into LateBoundBatchSpanProcessor
                LateBoundBatchSpanProcessor delayedProcessor = CDI.current()
                        .select(LateBoundBatchSpanProcessor.class, Any.Literal.INSTANCE).get();
                delayedProcessor.setBatchSpanProcessorDelegate(BatchSpanProcessor.builder(jaegerSpanExporter).build());
            } catch (IllegalArgumentException iae) {
                throw new IllegalStateException("Unable to install Jaeger Exporter", iae);
            }
        }
    }
}
