package io.quarkus.jaeger.runtime;

import org.jboss.logging.MDC;

import io.jaegertracing.internal.JaegerSpanContext;
import io.opentracing.Scope;
import io.opentracing.Span;

/**
 * Scope that sets span context into MDC.
 */
public class MDCScope implements Scope {

    /**
     * MDC keys
     */
    private static final String TRACE_ID = "traceId";
    private static final String SPAN_ID = "spanId";
    private static final String SAMPLED = "sampled";

    private final Scope wrapped;
    private final Scope toRestore;

    public MDCScope(Scope toRestore, Scope scope) {
        this.toRestore = toRestore;
        this.wrapped = scope;
        if (scope.span().context() instanceof JaegerSpanContext) {
            putContext((JaegerSpanContext) scope.span().context());
        }
    }

    @Override
    public void close() {
        wrapped.close();
        MDC.remove(TRACE_ID);
        MDC.remove(SPAN_ID);
        MDC.remove(SAMPLED);

        if (toRestore != null && toRestore.span().context() instanceof JaegerSpanContext) {
            putContext(((JaegerSpanContext) toRestore.span().context()));
        }
    }

    @Override
    public Span span() {
        return wrapped.span();
    }

    protected void putContext(JaegerSpanContext spanContext) {
        MDC.put(TRACE_ID, spanContext.getTraceId());
        MDC.put(SPAN_ID, String.format("%16x", spanContext.getSpanId()));
        MDC.put(SAMPLED, Boolean.toString(spanContext.isSampled()));
    }
}
