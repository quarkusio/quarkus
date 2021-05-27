package io.quarkus.opentelemetry.runtime;

import java.util.function.Supplier;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxInternal;

@Recorder
public class OpenTelemetryRecorder {

    /* STATIC INIT */
    public void resetGlobalOpenTelemetryForDevMode() {
        GlobalOpenTelemetry.resetForTest();
    }

    /* STATIC INIT */
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

    /* STATIC INIT */
    public void eagerlyCreateContextStorage() {
        ContextStorage.get();
    }

    /* RUNTIME INIT */
    public void storeVertxOnContextStorage(Supplier<Vertx> vertx) {
        QuarkusContextStorage.vertx = (VertxInternal) vertx.get();
    }
}
