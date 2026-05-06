package io.quarkus.opentelemetry.runtime.exporter.otlp.tracing;

import io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer.TracerProviderCustomizer;

/**
 * The only point in having this class is to allow {@link TracerProviderCustomizer}
 * to easily ignore the configured {@link LateBoundSpanProcessor}.
 *
 * Deprecated for removal. Check: https://github.com/quarkusio/quarkus/pull/52752
 */
@Deprecated(forRemoval = true)
public final class RemoveableLateBoundSpanProcessor extends LateBoundSpanProcessor {

    public static final RemoveableLateBoundSpanProcessor INSTANCE = new RemoveableLateBoundSpanProcessor();

    private RemoveableLateBoundSpanProcessor() {
        super(null);
    }
}
