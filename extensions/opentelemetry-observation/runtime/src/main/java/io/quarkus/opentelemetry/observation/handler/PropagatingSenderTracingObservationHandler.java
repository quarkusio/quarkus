package io.quarkus.opentelemetry.observation.handler;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;

import io.micrometer.observation.Observation;
import io.micrometer.observation.transport.SenderContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.quarkus.opentelemetry.observation.context.TracingObservationContext;

@Singleton
@Priority(ObservationHandlerPriorities.PROPAGATING_SENDER)
public class PropagatingSenderTracingObservationHandler
        extends AbstractTracingObservationHandler<SenderContext<?>> {

    private TextMapPropagator propagator;

    @PostConstruct
    void init() {
        this.propagator = openTelemetry.getPropagators().getTextMapPropagator();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void onStart(SenderContext<?> context) {
        super.onStart(context);

        TracingObservationContext tracingContext = context.getRequired(TracingObservationContext.class);
        Context otelContext = Context.current().with(tracingContext.getSpan());

        SenderContext rawContext = context;
        propagator.inject(otelContext, rawContext.getCarrier(), new TextMapSetter<Object>() {
            @Override
            public void set(Object carrier, String key, String value) {
                rawContext.getSetter().set(carrier, key, value);
            }
        });
    }

    @Override
    protected SpanKind spanKind() {
        return SpanKind.CLIENT;
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof SenderContext;
    }
}
