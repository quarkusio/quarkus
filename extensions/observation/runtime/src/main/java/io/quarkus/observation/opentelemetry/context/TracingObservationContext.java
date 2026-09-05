package io.quarkus.observation.opentelemetry.context;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;

/**
 * Span and scope are already managed by the OpenTelemetry extension QuarkusContextStorage.
 */
public class TracingObservationContext {

    // OTEL Span
    private Span span;
    // OTEL Scope
    private Scope scope;

    public Span getSpan() {
        return span;
    }

    public void setSpan(Span span) {
        this.span = span;
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    @Override
    public String toString() {
        return "TracingObservationContext{" +
                "span=" + span +
                ", scope=" + scope +
                '}';
    }
}
