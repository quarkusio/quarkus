package io.quarkus.opentelemetry;

import java.util.function.Consumer;

import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.quarkus.opentelemetry.tracing.vertx.VertxOpenTelemetryOptions;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.VertxOptions;

@Recorder
public class OpenTelemetryRecorder {
    public Consumer<VertxOptions> setVertxOpenTelemetryOptions() {
        return vertxOptions -> vertxOptions.setTracingOptions(new VertxOpenTelemetryOptions());
    }

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
