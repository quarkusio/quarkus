package io.quarkus.opentelemetry.exporter.otlp.runtime;

import java.util.Optional;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.CDI;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class OtlpRecorder {
    public void installBatchSpanProcessorForOtlp(OtlpExporterConfig.OtlpExporterRuntimeConfig runtimeConfig,
            LaunchMode launchMode) {
        if (launchMode == LaunchMode.DEVELOPMENT && !runtimeConfig.endpoint.isPresent()) {
            // Default the endpoint for development only
            runtimeConfig.endpoint = Optional.of("http://localhost:4317");
        }

        // Only create the OtlpGrpcSpanExporter if an endpoint was set in runtime config
        if (runtimeConfig.endpoint.isPresent() && runtimeConfig.endpoint.get().trim().length() > 0) {
            try {
                OtlpGrpcSpanExporter otlpSpanExporter = OtlpGrpcSpanExporter.builder()
                        .setEndpoint(runtimeConfig.endpoint.get())
                        .setTimeout(runtimeConfig.exportTimeout)
                        .build();

                // Create BatchSpanProcessor for OTLP and install into LateBoundBatchSpanProcessor
                LateBoundBatchSpanProcessor delayedProcessor = CDI.current()
                        .select(LateBoundBatchSpanProcessor.class, Any.Literal.INSTANCE).get();
                delayedProcessor.setBatchSpanProcessorDelegate(BatchSpanProcessor.builder(otlpSpanExporter).build());
            } catch (IllegalArgumentException iae) {
                throw new IllegalStateException("Unable to install OTLP Exporter", iae);
            }
        }
    }
}
