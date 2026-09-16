package io.quarkus.observation.opentelemetry.handler;

import java.util.Collections;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Singleton;

import io.micrometer.observation.Observation;
import io.micrometer.observation.transport.ReceiverContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.quarkus.observation.opentelemetry.context.TracingObservationContext;

@Singleton
@Priority(ObservationHandlerPriorities.PROPAGATING_RECEIVER)
@Typed(PropagatingReceiverTracingObservationHandler.class)
public class PropagatingReceiverTracingObservationHandler
        extends OpenTelemetryObservationHandler {

    private TextMapPropagator propagator;

    @PostConstruct
    void init() {
        this.propagator = openTelemetry.getPropagators().getTextMapPropagator();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected Context getParentContext(Observation.Context context) {
        if (!(context instanceof ReceiverContext)) {
            return super.getParentContext(context);
        }
        ReceiverContext rawContext = (ReceiverContext) context;
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
    protected void startSpan(Observation.Context observationContext) {
        if (observationContext instanceof ReceiverContext<?> receiverContext) {
            Context parentContext = getParentContext(observationContext);

            io.opentelemetry.api.trace.Span span = getTracer().spanBuilder(observationContext.getName())
                    .setParent(parentContext)
                    .setSpanKind(spanKind(receiverContext))
                    .startSpan();

            TracingObservationContext tracingContext = getTracingContext(observationContext);
            tracingContext.setSpan(span);
        } else {
            super.startSpan(observationContext);
        }
    }

    private SpanKind spanKind(ReceiverContext<?> context) {
        return switch (context.getKind()) {
            case CONSUMER -> SpanKind.CONSUMER;
            default -> SpanKind.SERVER;
        };
    }

    @Override
    public boolean supportsContext(Observation.Context observationContext) {
        return observationContext instanceof ReceiverContext;
    }
}
