package io.quarkus.opentelemetry.observation.handler;

import java.util.Collections;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;

import io.micrometer.observation.Observation;
import io.micrometer.observation.transport.ReceiverContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.quarkus.opentelemetry.observation.context.TracingObservationContext;

@Singleton
@Priority(ObservationHandlerPriorities.PROPAGATING_RECEIVER)
public class PropagatingReceiverTracingObservationHandler
        extends AbstractTracingObservationHandler<ReceiverContext<?>> {

    private TextMapPropagator propagator;

    @PostConstruct
    void init() {
        this.propagator = openTelemetry.getPropagators().getTextMapPropagator();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Context getParentContext(ReceiverContext<?> context) {
        @SuppressWarnings("rawtypes")
        ReceiverContext rawContext = context;
        return propagator.extract(Context.current(), rawContext.getCarrier(), new TextMapGetter<Object>() {
            @Override
            public Iterable<String> keys(Object carrier) {
                return Collections.emptyList();
            }

            @Override
            public String get(Object carrier, String key) {
                return rawContext.getGetter().get(carrier, key);
            }
        });
    }

    @Override
    public void onStart(ReceiverContext<?> context) {
        Context parentContext = getParentContext(context);

        TracingObservationContext tracingContext = new TracingObservationContext();
        tracingContext.setParentContext(parentContext);

        io.opentelemetry.api.trace.Span span = getTracer().spanBuilder(context.getName())
                .setParent(parentContext)
                .setSpanKind(spanKind(context))
                .startSpan();

        tracingContext.setSpan(span);
        context.put(TracingObservationContext.class, tracingContext);
    }

    @Override
    protected SpanKind spanKind() {
        return SpanKind.SERVER;
    }

    private SpanKind spanKind(ReceiverContext<?> context) {
        return switch (context.getKind()) {
            case CONSUMER -> SpanKind.CONSUMER;
            default -> SpanKind.SERVER;
        };
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ReceiverContext;
    }
}
