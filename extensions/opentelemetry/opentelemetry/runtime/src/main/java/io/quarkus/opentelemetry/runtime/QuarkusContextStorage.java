package io.quarkus.opentelemetry.runtime;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.vertx.core.Vertx;

public enum QuarkusContextStorage implements ContextStorage {
    INSTANCE;

    private static final Logger log = Logger.getLogger(QuarkusContextStorage.class);

    private static final String TRACE_ID = "traceId";
    private static final String SPAN_ID = "spanId";
    private static final String SAMPLED = "sampled";
    private static final String PARENT_ID = "parentId";

    public static final String ACTIVE_CONTEXT = QuarkusContextStorage.class.getName() + ".activeContext";

    static Vertx vertx;

    @Override
    public Scope attach(Context toAttach) {
        return attach(getVertxContext(), toAttach);
    }

    public Scope attach(io.vertx.core.Context vertxContext, Context toAttach) {
        if (toAttach == null) {
            // Not allowed
            return Scope.noop();
        }

        Context beforeAttach = getContext(vertxContext);
        if (toAttach == beforeAttach) {
            return Scope.noop();
        }

        if (vertxContext != null) {
            vertxContext.putLocal(ACTIVE_CONTEXT, toAttach);
            setMDC(toAttach);

            return () -> {
                if (getContext(vertxContext) != toAttach) {
                    log.warn("Context in storage not the expected context, Scope.close was not called correctly");
                }
                if (beforeAttach == null) {
                    vertxContext.removeLocal(ACTIVE_CONTEXT);
                    clearMDC();
                } else {
                    vertxContext.putLocal(ACTIVE_CONTEXT, beforeAttach);
                    setMDC(beforeAttach);
                }
            };
        }

        return Scope.noop();
    }

    @Override
    public Context current() {
        return getContext(getVertxContext());
    }

    private Context getContext(io.vertx.core.Context vertxContext) {
        return vertxContext != null ? vertxContext.getLocal(ACTIVE_CONTEXT) : null;
    }

    private io.vertx.core.Context getVertxContext() {
        return vertx.getOrCreateContext();
    }

    private void setMDC(Context context) {
        Span span = Span.fromContextOrNull(context);
        if (span != null) {
            SpanContext spanContext = span.getSpanContext();
            MDC.put(TRACE_ID, spanContext.getTraceId());
            MDC.put(SPAN_ID, spanContext.getSpanId());
            MDC.put(SAMPLED, Boolean.toString(spanContext.isSampled()));
            if (span instanceof ReadableSpan) {
                SpanContext parentSpanContext = ((ReadableSpan) span).getParentSpanContext();
                if (parentSpanContext.isValid()) {
                    MDC.put(PARENT_ID, parentSpanContext.getSpanId());
                } else {
                    MDC.remove(PARENT_ID);
                }
            }
        }
    }

    private void clearMDC() {
        MDC.remove(TRACE_ID);
        MDC.remove(SPAN_ID);
        MDC.remove(PARENT_ID);
        MDC.remove(SAMPLED);
    }
}
