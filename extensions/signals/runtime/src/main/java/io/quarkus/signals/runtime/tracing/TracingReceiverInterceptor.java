package io.quarkus.signals.runtime.tracing;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
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
 * Built-in interceptor that creates a {@link SpanKind#INTERNAL} span for each receiver invocation. If the
 * {@link TracingSignalMetadataEnricher} propagated a valid trace context, the span becomes a child of the emitter's
 * span; otherwise a new root span is started.
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

    // The span name is "receive <raw signal type FQCN>"; the (possibly parameterized) signal type is also recorded in
    // the "signals.signal.type" attribute (cardinality is bounded by the number of distinct signal types)
    private static final String SPAN_NAME_PREFIX = "receive ";

    private static final AttributeKey<List<String>> QUALIFIERS = AttributeKey.stringArrayKey("signals.qualifiers");

    private static final Logger LOG = Logger.getLogger(TracingReceiverInterceptor.class);

    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;

    TracingReceiverInterceptor(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        this.tracer = openTelemetry.getTracer(TracingSupport.TRACER_NAME);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Uni<Object> intercept(InterceptionContext context) {
        SignalContext<?> signalContext = context.signalContext();

        // INTERNAL is used deliberately: signals are delivered in-process (there is no messaging broker) and we do not
        // emit the messaging.* semantic conventions nor a PRODUCER counterpart span, so CONSUMER would over-claim
        // messaging semantics. This may be revisited if signals gain full messaging instrumentation.
        SpanBuilder spanBuilder = tracer.spanBuilder(SPAN_NAME_PREFIX + rawTypeName(signalContext.signalType()))
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("signals.signal.type", signalContext.signalType().getTypeName())
                .setAttribute("signals.emission.type", signalContext.emissionType().toString());
        // Use the propagated trace context as the parent if available; otherwise start a new root span
        Object carrier = signalContext.metadata().get(TracingSupport.TRACE_CONTEXT_METADATA_KEY);
        if (carrier instanceof Map map) {
            Context parentContext = openTelemetry.getPropagators().getTextMapPropagator()
                    .extract(Context.root(), map, TracingSupport.GETTER);
            // A valid parent span makes the receiver span its child; otherwise (e.g. only baggage) start a root span
            if (Span.fromContext(parentContext).getSpanContext().isValid()) {
                spanBuilder.setParent(parentContext);
            } else {
                spanBuilder.setNoParent();
            }
        } else {
            spanBuilder.setNoParent();
        }
        if (signalContext.responseType() != null) {
            spanBuilder.setAttribute("signals.response.type", signalContext.responseType().getTypeName());
        }
        List<String> qualifiers = new ArrayList<>();
        for (Annotation qualifier : signalContext.qualifiers()) {
            String name = qualifier.annotationType().getName();
            if (!Default.class.getName().equals(name) && !Any.class.getName().equals(name)) {
                qualifiers.add(name);
            }
        }
        if (!qualifiers.isEmpty()) {
            spanBuilder.setAttribute(QUALIFIERS, qualifiers);
        }
        String receiverName = context.receiver().name();
        if (receiverName != null) {
            spanBuilder.setAttribute("signals.receiver", receiverName);
        }
        Span span = spanBuilder.startSpan();
        // Report the time elapsed between the emission and the start of this receiver invocation, if available
        Object emitNanos = signalContext.metadata().get(TracingSupport.EMIT_NANOS_METADATA_KEY);
        if (emitNanos instanceof Long) {
            long waitNanos = System.nanoTime() - (Long) emitNanos;
            if (waitNanos >= 0) {
                span.setAttribute("signals.receiver.wait_ms", TimeUnit.NANOSECONDS.toMillis(waitNanos));
            }
        }
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
                            span.setAttribute("error.type", failure.getClass().getName());
                        }
                    } finally {
                        scope.close();
                        span.end();
                    }
                });
            }
        });
    }

    private static String rawTypeName(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            return parameterizedType.getRawType().getTypeName();
        }
        return type.getTypeName();
    }
}
