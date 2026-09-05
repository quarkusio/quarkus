package io.quarkus.signals.runtime.tracing;

import java.util.Map;
import java.util.function.Supplier;

import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.quarkus.signals.SignalContext;
import io.quarkus.signals.spi.ReceiverInterceptor;
import io.quarkus.signals.spi.RelativeOrder;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;

/**
 * Built-in interceptor that restores the OpenTelemetry trace context captured by {@link TracingSignalMetadataEnricher}
 * and creates a {@link SpanKind#INTERNAL} span for each receiver invocation.
 * <p>
 * The interceptor is ordered before {@link #ID_REQUEST_CONTEXT} so that the span encloses the whole receiver invocation.
 * The span is made current at subscription time so that instrumentation nested in the receiver (e.g. {@code @WithSpan}
 * methods) is correlated, and it is ended when the receiver completes, recording the exception and an error status on
 * failure.
 * <p>
 * It is only registered when the OpenTelemetry tracer capability is present.
 */
@Identifier(TracingReceiverInterceptor.ID)
@RelativeOrder(before = ReceiverInterceptor.ID_REQUEST_CONTEXT)
@Singleton
public class TracingReceiverInterceptor implements ReceiverInterceptor {

    public static final String ID = "quarkus.tracing";

    // A low-cardinality, constant span name; the concrete signal type is recorded in the "signals.signal.type" attribute
    private static final String SPAN_NAME = "signal receive";

    private static final Logger LOG = Logger.getLogger(TracingReceiverInterceptor.class);

    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;

    TracingReceiverInterceptor(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        this.tracer = openTelemetry.getTracer(TracingSupport.TRACER_NAME);
    }

    @Override
    public Uni<Object> intercept(InterceptionContext context) {
        SignalContext<?> signalContext = context.signalContext();
        Object carrier = signalContext.metadata().get(TracingSupport.METADATA_KEY);
        if (!(carrier instanceof Map)) {
            // No trace context was propagated (e.g. the signal was emitted outside of an active trace)
            return context.proceed();
        }
        @SuppressWarnings("unchecked")
        Map<String, String> traceContext = (Map<String, String>) carrier;
        Context parentContext = openTelemetry.getPropagators().getTextMapPropagator()
                .extract(Context.root(), traceContext, TracingSupport.GETTER);

        if (!Span.fromContext(parentContext).getSpanContext().isValid()) {
            // No valid parent span (e.g. only baggage was propagated); do not create an orphan span
            return context.proceed();
        }

        // INTERNAL is used deliberately: signals are delivered in-process (there is no messaging broker) and we do not
        // emit the messaging.* semantic conventions nor a PRODUCER counterpart span, so CONSUMER would over-claim
        // messaging semantics. This may be revisited if signals gain full messaging instrumentation.
        Span span = tracer.spanBuilder(SPAN_NAME)
                .setParent(parentContext)
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("signals.signal.type", signalContext.signalType().getTypeName())
                .setAttribute("signals.emission.type", signalContext.emissionType().toString())
                .startSpan();
        LOG.debugf("Started span %s for receiver %s", span.getSpanContext().getSpanId(), context.receiver());

        return Uni.createFrom().deferred(new Supplier<Uni<? extends Object>>() {
            @Override
            public Uni<Object> get() {
                // makeCurrent() attaches to the current (duplicated) context on the receiver's execution thread
                Scope scope = span.makeCurrent();
                return context.proceed().onItemOrFailure().invoke((item, failure) -> {
                    try {
                        if (failure != null) {
                            span.recordException(failure);
                            span.setStatus(StatusCode.ERROR);
                        }
                    } finally {
                        scope.close();
                        span.end();
                    }
                });
            }
        });
    }
}
