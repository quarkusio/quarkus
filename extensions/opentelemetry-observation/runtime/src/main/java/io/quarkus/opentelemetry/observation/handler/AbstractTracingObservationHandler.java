package io.quarkus.opentelemetry.observation.handler;

import static io.smallrye.common.vertx.VertxContext.isDuplicatedContext;

import java.util.Map;

import jakarta.inject.Inject;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.opentelemetry.observation.context.TracingObservationContext;
import io.quarkus.opentelemetry.observation.context.TypedAttributes;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;
import io.smallrye.common.vertx.VertxContext;

public abstract class AbstractTracingObservationHandler<T extends Observation.Context>
        implements ObservationHandler<T> {

    public static final String OBSERVATION_KEY = AbstractTracingObservationHandler.class.getName() + ".observation";
    private static final String INSTRUMENTATION_NAME = "io.quarkus.opentelemetry";

    @Inject
    OpenTelemetry openTelemetry;

    protected Tracer getTracer() {
        return openTelemetry.getTracer(INSTRUMENTATION_NAME);
    }

    @Override
    public void onStart(T context) {
        Context parentContext = getParentContext(context);
        SpanBuilder spanBuilder = getTracer().spanBuilder(context.getName())
                .setParent(parentContext)
                .setSpanKind(spanKind());

        Span span = spanBuilder.startSpan();

        TracingObservationContext tracingContext = new TracingObservationContext();
        tracingContext.setSpan(span);
        tracingContext.setParentContext(parentContext);
        context.put(TracingObservationContext.class, tracingContext);
    }

    @Override
    public void onScopeOpened(T context) {
        TracingObservationContext tracingContext = context.getRequired(TracingObservationContext.class);
        Span span = tracingContext.getSpan();

        Scope otelScope = Context.current().with(span).makeCurrent();
        tracingContext.setScope(otelScope);

        // Store Observation in duplicated context for cross-thread propagation
        io.vertx.core.Context vertxCtx = QuarkusContextStorage.getVertxContext();
        if (vertxCtx != null && isDuplicatedContext(vertxCtx)) {
            Observation currentObs = getCurrentObservation();
            Map<String, Object> localData = VertxContext.localContextData(vertxCtx);
            Object previous = localData.put(OBSERVATION_KEY, currentObs);
            tracingContext.setPreviousObservation(previous);
        }
    }

    @Override
    public void onScopeClosed(T context) {
        TracingObservationContext tracingContext = context.getRequired(TracingObservationContext.class);

        Scope scope = tracingContext.getScope();
        if (scope != null) {
            scope.close();
        }

        // Restore previous Observation in duplicated context
        io.vertx.core.Context vertxCtx = QuarkusContextStorage.getVertxContext();
        if (vertxCtx != null && isDuplicatedContext(vertxCtx)) {
            Object previous = tracingContext.getPreviousObservation();
            Map<String, Object> localData = VertxContext.localContextData(vertxCtx);
            if (previous == null) {
                localData.remove(OBSERVATION_KEY);
            } else {
                localData.put(OBSERVATION_KEY, previous);
            }
        }
    }

    @Override
    public void onError(T context) {
        TracingObservationContext tracingContext = context.getRequired(TracingObservationContext.class);
        Span span = tracingContext.getSpan();
        Throwable error = context.getError();
        if (span != null && error != null) {
            span.recordException(error);
            span.setStatus(StatusCode.ERROR,
                    error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName());
        }
    }

    @Override
    public void onEvent(Observation.Event event, T context) {
        TracingObservationContext tracingContext = context.getRequired(TracingObservationContext.class);
        Span span = tracingContext.getSpan();
        if (span != null) {
            span.addEvent(event.getName());
        }
    }

    @Override
    public void onStop(T context) {
        TracingObservationContext tracingContext = context.getRequired(TracingObservationContext.class);
        Span span = tracingContext.getSpan();
        if (span == null) {
            return;
        }

        tagSpan(context, span);

        String contextualName = context.getContextualName();
        if (contextualName != null) {
            span.updateName(contextualName);
        }

        if (context.getError() == null) {
            span.setStatus(StatusCode.OK);
        }

        span.end();
    }

    protected Context getParentContext(T context) {
        return Context.current();
    }

    protected SpanKind spanKind() {
        return SpanKind.INTERNAL;
    }

    private Observation getCurrentObservation() {
        InstanceHandle<io.micrometer.observation.ObservationRegistry> handle = Arc.container()
                .instance(io.micrometer.observation.ObservationRegistry.class);
        if (handle.isAvailable()) {
            return handle.get().getCurrentObservation();
        }
        return null;
    }

    protected void tagSpan(T context, Span span) {
        // Low-cardinality key-values as span attributes
        context.getLowCardinalityKeyValues().forEach(kv -> span.setAttribute(kv.getKey(), kv.getValue()));

        // High-cardinality key-values as span attributes
        context.getHighCardinalityKeyValues().forEach(kv -> span.setAttribute(kv.getKey(), kv.getValue()));

        // Typed attributes
        TypedAttributes typedAttributes = context.get(TypedAttributes.class);
        if (typedAttributes != null) {
            Attributes attrs = typedAttributes.build();
            attrs.forEach((key, value) -> span.setAttribute((io.opentelemetry.api.common.AttributeKey) key, value));
        }
    }
}
