package io.quarkus.opentelemetry.observation.context;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

public class TracingObservationContext {

    private Span span;
    private SpanBuilder spanBuilder;
    private Context parentContext;
    private Scope scope;
    private Object previousObservation;

    public Span getSpan() {
        return span;
    }

    public void setSpan(Span span) {
        this.span = span;
    }

    public SpanBuilder getSpanBuilder() {
        return spanBuilder;
    }

    public void setSpanBuilder(SpanBuilder spanBuilder) {
        this.spanBuilder = spanBuilder;
    }

    public Context getParentContext() {
        return parentContext;
    }

    public void setParentContext(Context parentContext) {
        this.parentContext = parentContext;
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public Object getPreviousObservation() {
        return previousObservation;
    }

    public void setPreviousObservation(Object previousObservation) {
        this.previousObservation = previousObservation;
    }
}
