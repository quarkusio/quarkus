package io.quarkus.opentelemetry.runtime.exporter.otlp.tracing;

import io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer.TracerProviderCustomizer;

/**
 * The only point in having this class is to allow {@link TracerProviderCustomizer}
 * to easily ignore the configured {@link LateBoundBatchSpanProcessor}.
 */
public final class RemoveableLateBoundBatchSpanProcessor extends LateBoundBatchSpanProcessor {

    public static final RemoveableLateBoundBatchSpanProcessor INSTANCE = new RemoveableLateBoundBatchSpanProcessor();

    private RemoveableLateBoundBatchSpanProcessor() {
        super(null);
    }
}
