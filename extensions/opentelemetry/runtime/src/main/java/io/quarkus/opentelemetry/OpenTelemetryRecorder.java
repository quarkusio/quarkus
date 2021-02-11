package io.quarkus.opentelemetry;

import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class OpenTelemetryRecorder {
    public void createOpenTelemetry(RuntimeValue<SdkTracerProvider> tracerProvider) {
        OpenTelemetrySdkBuilder builder = OpenTelemetrySdk.builder();

        // Set tracer provider if present
        if (tracerProvider != null) {
            builder.setTracerProvider(tracerProvider.getValue());
        }

        // Add propagators. //TODO Need a way to handle this with config
        builder.setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()));

        builder.buildAndRegisterGlobal();
    }
}
