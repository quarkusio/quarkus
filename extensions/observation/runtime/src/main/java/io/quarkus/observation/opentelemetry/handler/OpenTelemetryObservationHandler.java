package io.quarkus.observation.opentelemetry.handler;

import java.util.function.BiConsumer;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.micrometer.observation.Observation;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.quarkus.observation.handler.AbstractTracingObservationHandler;
import io.quarkus.observation.opentelemetry.context.TracingObservationContext;
import io.quarkus.observation.opentelemetry.context.TypedAttributes;

@Singleton
@Priority(ObservationHandlerPriorities.DEFAULT_TRACING)
public class OpenTelemetryObservationHandler
        extends AbstractTracingObservationHandler<Observation.Context> {

    private static final String INSTRUMENTATION_NAME = "io.quarkus.opentelemetry";

    @Inject
    OpenTelemetry openTelemetry;

    protected Tracer getTracer() {
        return openTelemetry.getTracer(INSTRUMENTATION_NAME);
    }

    private SpanKind spanKind() {
        return SpanKind.INTERNAL;
    }

    protected Context getParentContext(Observation.Context context) {
        return Context.current();
    }

    @Override
    protected void startSpan(Observation.Context observationContext) {
        Context parentOtelContext = getParentContext(observationContext);
        Span span = getTracer().spanBuilder(observationContext.getName())
                .setParent(parentOtelContext)
                .setSpanKind(spanKind())
                .startSpan();

        TracingObservationContext tracingContext = getTracingContext(observationContext);
        tracingContext.setSpan(span);
    }

    @Override
    protected void openScope(Observation.Context observationContext) {
        TracingObservationContext tracingContext = getTracingContext(observationContext);
        Span span = tracingContext.getSpan();
        // See OTel impl will set OTel context in vertx.
        Scope otelScope = Context.current().with(span).makeCurrent();
        tracingContext.setScope(otelScope);
    }

    @Override
    protected void closeScope(Observation.Context observationContext) {
        TracingObservationContext tracingContext = getTracingContext(observationContext);
        Scope otelScope = tracingContext.getScope();
        if (otelScope != null) {
            otelScope.close();
        }
    }

    @Override
    protected void recordError(Observation.Context observationContext, Throwable error) {
        TracingObservationContext tracingContext = getTracingContext(observationContext);
        Span span = tracingContext.getSpan();
        if (span != null && error != null) {
            span.recordException(error);
            span.setStatus(StatusCode.ERROR,
                    error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName());
        }
    }

    @Override
    protected void recordEvent(Observation.Event observationEvent, Observation.Context observationContext) {
        TracingObservationContext tracingContext = getTracingContext(observationContext);
        Span span = tracingContext.getSpan();
        if (span != null) {
            span.addEvent(observationEvent.getName());
        }
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void stopSpan(Observation.Context observationContext) {
        TracingObservationContext tracingContext = getTracingContext(observationContext);
        Span span = tracingContext.getSpan();
        if (span == null) {
            return;
        }

        tagSpan(observationContext, span);

        String contextualName = observationContext.getContextualName();
        if (contextualName != null) {
            span.updateName(contextualName);
        }

        if (observationContext.getError() == null) {
            span.setStatus(StatusCode.OK);
        }

        span.end();
    }

    @Override
    public boolean supportsContext(Observation.Context observationContext) {
        return true;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void tagSpan(Observation.Context observationContext, Span span) {
        observationContext.getLowCardinalityKeyValues().forEach(kv -> span.setAttribute(kv.getKey(), kv.getValue()));
        observationContext.getHighCardinalityKeyValues().forEach(kv -> span.setAttribute(kv.getKey(), kv.getValue()));

        TypedAttributes typedAttributes = observationContext.get(TypedAttributes.class);
        if (typedAttributes != null) {
            Attributes otelAttrs = typedAttributes.build();
            otelAttrs.forEach(new BiConsumer<AttributeKey<?>, Object>() {
                @Override
                public void accept(AttributeKey<?> key, Object value) {
                    span.setAttribute((AttributeKey) key, value);
                }
            });
        }
    }

    protected TracingObservationContext getTracingContext(Observation.Context context) {
        return context.computeIfAbsent(TracingObservationContext.class, clazz -> new TracingObservationContext());
    }
}
