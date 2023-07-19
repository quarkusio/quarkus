package io.quarkus.opentelemetry.runtime.exporter.otlp;

import io.quarkus.opentelemetry.runtime.OpenTelemetryProducer;

/**
 * The only point in having this class is to allow {@link OpenTelemetryProducer}
 * to easily ignore the configured {@link LateBoundBatchSpanProcessor}.
 * <p>
 * In the future when {@link OpenTelemetryProducer} is replaced by a synthetic bean, this class will no longer be necessary
 */
public final class RemoveableLateBoundBatchSpanProcessor extends LateBoundBatchSpanProcessor {

    public static final RemoveableLateBoundBatchSpanProcessor INSTANCE = new RemoveableLateBoundBatchSpanProcessor();

    private RemoveableLateBoundBatchSpanProcessor() {
        super(null);
    }
}
