package io.quarkus.opentelemetry.exporter.otlp.http.runtime;

import java.util.Map;
import java.util.Optional;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.CDI;

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.quarkus.opentelemetry.runtime.OpenTelemetryUtil;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class OtlpHttpRecorder {
    public void installBatchSpanProcessorForOtlpHttp(OtlpHttpExporterConfig.OtlpHttpExporterRuntimeConfig runtimeConfig,
            LaunchMode launchMode) {
        if (launchMode == LaunchMode.DEVELOPMENT && !runtimeConfig.endpoint.isPresent()) {
            // Default the endpoint for development only
            runtimeConfig.endpoint = Optional.of("http://localhost:4318");
        }
        // Only create the OtlpHttpSpanExporter if an endpoint was set in runtime config
        if (runtimeConfig.endpoint.isPresent() && runtimeConfig.endpoint.get().trim().length() > 0) {
            try {
                OtlpHttpSpanExporterBuilder otlpHttpSpanExporterBuilder = OtlpHttpSpanExporter.builder()
                        .setEndpoint(runtimeConfig.endpoint.get())
                        .setTimeout(runtimeConfig.exportTimeout);
                if (runtimeConfig.headers.isPresent()) {
                    Map<String, String> headers = OpenTelemetryUtil.convertKeyValueListToMap(runtimeConfig.headers.get());
                    headers.forEach(otlpHttpSpanExporterBuilder::addHeader);
                }

                runtimeConfig.compression.ifPresent(otlpHttpSpanExporterBuilder::setCompression);

                OtlpHttpSpanExporter otlpHttpSpanExporter = otlpHttpSpanExporterBuilder.build();

                // Create BatchSpanProcessor for OTLP/HTTP and install into LateBoundBatchSpanProcessor
                LateBoundBatchSpanProcessor delayedProcessor = CDI.current()
                        .select(LateBoundBatchSpanProcessor.class, Any.Literal.INSTANCE).get();
                delayedProcessor.setBatchSpanProcessorDelegate(BatchSpanProcessor.builder(otlpHttpSpanExporter).build());
            } catch (IllegalArgumentException iae) {
                throw new IllegalStateException("Unable to install OTLP/HTTP Exporter", iae);
            }
        }
    }
}
