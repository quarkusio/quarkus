package io.quarkus.opentelemetry.runtime.exporter.otlp.tracing;

import io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer.TracerProviderCustomizer;

/**
 * The only point in having this class is to allow {@link TracerProviderCustomizer}
 * to easily ignore the configured {@link LateBoundSpanProcessor}.
 */
public final class RemoveableLateBoundSpanProcessor extends LateBoundSpanProcessor {

    public static final RemoveableLateBoundSpanProcessor INSTANCE = new RemoveableLateBoundSpanProcessor();

    private RemoveableLateBoundSpanProcessor() {
        super(null);
    }
}
