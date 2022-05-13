package io.quarkus.opentelemetry.async.mutiny.runtime.tracing;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.quarkus.opentelemetry.async.mutiny.runtime.MutinyAsyncConfig;
import io.smallrye.mutiny.tuples.Functions;

/**
 * Here we handle termination of Uni and Multi. We make sure that spans are closed only once and failures are handled
 * properly
 *
 * @param <T> response type for instrumenter
 */
public abstract class EndOnFirstNotificationConsumer<T> implements
        // For Uni termination
        Functions.TriConsumer<T, Throwable, Boolean>,
        // For Multi termination
        BiConsumer<Throwable, Boolean> {

    private static final AttributeKey<Boolean> CANCELED_ATTRIBUTE_KEY = AttributeKey.booleanKey("mutiny.canceled");

    private final AtomicBoolean firstTermination = new AtomicBoolean();
    private final MutinyAsyncConfig.MutinyAsyncRuntimeConfig config;
    private final Context context;

    protected EndOnFirstNotificationConsumer(final Context context,
            final MutinyAsyncConfig.MutinyAsyncRuntimeConfig config) {
        this.context = context;
        this.config = config;
    }

    public Context getContext() {
        return context;
    }

    /**
     * Handles termination of multi. See:
     * {@link io.smallrye.mutiny.groups.MultiOnTerminate#invoke(BiConsumer)}}
     *
     * @param failure failure or <code>null</code>
     * @param isCancelled <code>true</code> if stream is cancelled. Otherwise, <code>false</code>
     */
    @Override
    public void accept(final Throwable failure, final Boolean isCancelled) {
        accept(null, failure, isCancelled);
    }

    /**
     * Handles termination of uni and multi. See:
     * {@link io.smallrye.mutiny.groups.UniOnTerminate#invoke(Functions.TriConsumer)}} or
     * {@link io.smallrye.mutiny.groups.MultiOnTerminate#invoke(BiConsumer)}}
     *
     * @param item item or <code>null</code>
     * @param failure failure or <code>null</code>
     * @param isCancelled <code>true</code> if stream is cancelled. Otherwise, <code>false</code>
     */
    @Override
    public void accept(final T item, final Throwable failure, final Boolean isCancelled) {
        if (firstTermination.compareAndSet(false, true)) {
            end(item, getFailure(failure, isCancelled));
        }
    }

    private Throwable getFailure(final Throwable failure, final Boolean isCancelled) {
        if (Boolean.TRUE.equals(isCancelled)) {
            if (Boolean.TRUE.equals(this.config.spanAttribute.onCancellation)) {
                Span.fromContext(context).setAttribute(CANCELED_ATTRIBUTE_KEY, true);
            }
            return new CancellationException("Mutiny stream cancelled");
        }
        return failure;
    }

    protected abstract void end(Object itemOrNull, Throwable failureOrNull);
}
