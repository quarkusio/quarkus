package io.quarkus.opentelemetry.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;

public class TracingUtils {
    public static final String SPAN_KEY = Span.class.getName() + ".activeSpan";
    public static final String SCOPE_KEY = Scope.class.getName() + ".activeScope";
}
