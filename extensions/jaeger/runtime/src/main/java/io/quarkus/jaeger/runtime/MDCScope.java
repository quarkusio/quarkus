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
    private static final String PARENT_ID = "parentId";
    private static final String SAMPLED = "sampled";

    private final Scope wrapped;
    private final Object originalTraceId;
    private final Object originalSpanId;
    private final Object originalParentId;
    private final Object originalSampled;

    public MDCScope(Scope scope, Span span) {
        this.wrapped = scope;
        this.originalTraceId = MDC.get(TRACE_ID);
        this.originalSpanId = MDC.get(SPAN_ID);
        this.originalParentId = MDC.get(PARENT_ID);
        this.originalSampled = MDC.get(SAMPLED);
        if (span.context() instanceof JaegerSpanContext) {
            putContext((JaegerSpanContext) span.context());
        }
    }

    @Override
    public void close() {
        wrapped.close();
        MDC.remove(TRACE_ID);
        MDC.remove(SPAN_ID);
        MDC.remove(PARENT_ID);
        MDC.remove(SAMPLED);

        if (originalTraceId != null) {
            MDC.put(TRACE_ID, originalTraceId);
        }
        if (originalSpanId != null) {
            MDC.put(SPAN_ID, originalSpanId);
        }
        if (originalParentId != null) {
            MDC.put(PARENT_ID, originalParentId);
        }
        if (originalSampled != null) {
            MDC.put(SAMPLED, originalSampled);
        }
    }

    protected void putContext(JaegerSpanContext spanContext) {
        MDC.put(TRACE_ID, spanContext.getTraceId());
        MDC.put(SPAN_ID, Long.toHexString(spanContext.getSpanId()));
        MDC.put(PARENT_ID, Long.toHexString(spanContext.getParentId()));
        MDC.put(SAMPLED, Boolean.toString(spanContext.isSampled()));
    }
}
