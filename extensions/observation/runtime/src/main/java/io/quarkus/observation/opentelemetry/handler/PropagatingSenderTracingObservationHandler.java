package io.quarkus.observation.opentelemetry.handler;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Singleton;

import io.micrometer.observation.Observation;
import io.micrometer.observation.transport.SenderContext;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.quarkus.observation.opentelemetry.context.TracingObservationContext;

@Singleton
@Priority(ObservationHandlerPriorities.PROPAGATING_SENDER)
@Typed(PropagatingSenderTracingObservationHandler.class)
public class PropagatingSenderTracingObservationHandler
        extends OpenTelemetryObservationHandler {

    private TextMapPropagator propagator;

    @PostConstruct
    void init() {
        this.propagator = openTelemetry.getPropagators().getTextMapPropagator();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected void startSpan(Observation.Context observationContext) {
        Context parentContext = getParentContext(observationContext);

        Span span = getTracer().spanBuilder(observationContext.getName())
                .setParent(parentContext)
                .setSpanKind(spanKind(observationContext))
                .startSpan();

        TracingObservationContext tracingContext = getTracingContext(observationContext);
        tracingContext.setSpan(span);

        if (observationContext instanceof SenderContext senderCtx) {
            Context otelContext = Context.current().with(span);
            propagator.inject(otelContext, senderCtx.getCarrier(), new TextMapSetter<Object>() {
                @Override
                public void set(Object carrier, String key, String value) {
                    senderCtx.getSetter().set(carrier, key, value);
                }
            });
        }
    }

    private SpanKind spanKind(Observation.Context context) {
        if (context instanceof SenderContext<?> senderContext) {
            return switch (senderContext.getKind()) {
                case PRODUCER -> SpanKind.PRODUCER;
                default -> SpanKind.CLIENT;
            };
        }
        return SpanKind.CLIENT;
    }

    @Override
    public boolean supportsContext(Observation.Context observationContext) {
        return observationContext instanceof SenderContext;
    }
}
