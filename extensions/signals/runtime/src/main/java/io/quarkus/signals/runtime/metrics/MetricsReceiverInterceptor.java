package io.quarkus.signals.runtime.metrics;

import java.util.function.Supplier;

import jakarta.inject.Singleton;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter.MeterProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.quarkus.signals.SignalContext;
import io.quarkus.signals.spi.ReceiverInterceptor;
import io.quarkus.signals.spi.RelativeOrder;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;

/**
 * Built-in interceptor that increments the {@value MetricsSupport#RECEIVER_EXECUTIONS} counter once per completed
 * receiver invocation. The outcome is distinguished by the {@value MetricsSupport#TAG_ERROR_TYPE} tag, which is set to
 * {@value MetricsSupport#ERROR_TYPE_NONE} on success and to the exception class name on failure.
 * <p>
 * The interceptor is ordered before {@link #ID_REQUEST_CONTEXT} so that an invocation is counted even if the request
 * context activation or the receiver itself fails. The counter is incremented when the invocation completes, so it
 * reflects the receiver actually running.
 * <p>
 * The meter is deliberately not tagged with the receiver identity: programmatic receivers may be anonymous and may be
 * registered and unregistered at runtime, which would produce {@code null} tag values and unbounded cardinality.
 * Receiver-level detail is available through tracing spans instead.
 * <p>
 * It is only registered when the Micrometer metrics capability is present.
 */
@Identifier(MetricsReceiverInterceptor.ID)
@RelativeOrder(before = ReceiverInterceptor.ID_REQUEST_CONTEXT)
@Singleton
public class MetricsReceiverInterceptor implements ReceiverInterceptor {

    public static final String ID = "quarkus.metrics";

    private final MeterProvider<Counter> executions;

    MetricsReceiverInterceptor(MeterRegistry registry) {
        this.executions = Counter.builder(MetricsSupport.RECEIVER_EXECUTIONS).withRegistry(registry);
    }

    @Override
    public Uni<Object> intercept(InterceptionContext context) {
        SignalContext<?> signalContext = context.signalContext();
        String signalType = MetricsSupport.rawTypeName(signalContext.signalType());
        String emissionType = signalContext.emissionType().toString();

        // intercept() runs at assembly time (when the Uni pipeline is built), whereas the receiver only runs at
        // subscription time. proceed() is wrapped in deferred() and the counter is incremented in onItemOrFailure() so
        // that it happens once per actual execution: it is not incremented for a Uni that is never subscribed, it is
        // incremented again on a re-subscription, and the outcome (success or the exception class) is known at that point.
        return Uni.createFrom().deferred(new Supplier<Uni<? extends Object>>() {
            @Override
            public Uni<Object> get() {
                return context.proceed().onItemOrFailure().invoke((item, failure) -> {
                    String errorType = failure != null ? failure.getClass().getName() : MetricsSupport.ERROR_TYPE_NONE;
                    executions.withTags(Tags.of(
                            MetricsSupport.TAG_SIGNAL_TYPE, signalType,
                            MetricsSupport.TAG_EMISSION_TYPE, emissionType,
                            MetricsSupport.TAG_ERROR_TYPE, errorType)).increment();
                });
            }
        });
    }
}
