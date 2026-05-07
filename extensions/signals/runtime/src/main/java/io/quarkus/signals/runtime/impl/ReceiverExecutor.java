package io.quarkus.signals.runtime.impl;

import io.quarkus.signals.SignalContext;
import io.quarkus.signals.spi.Receiver;
import io.quarkus.signals.spi.Receiver.ExecutionModel;
import io.smallrye.mutiny.Uni;

/**
 * Executes a {@link Receiver} with the given {@link SignalContext}.
 * <p>
 * This interface is pluggable &mdash; other Quarkus extensions can provide a custom implementation as a CDI bean to
 * override the default behavior. The default implementation is based on Vert.x and respects the receiver's
 * {@link Receiver.ExecutionModel execution model} (event loop, worker thread, or virtual thread).
 *
 * @see Receiver.ExecutionModel
 */
public interface ReceiverExecutor {

    /**
     * @param val
     * @return {@code true} if the specified execution model is supported, {@code false} otherwise
     */
    boolean supportsExecutionModel(ExecutionModel val);

    /**
     * Executes the given receiver with the provided signal context.
     *
     * @param <SIGNAL> the signal type
     * @param <RESPONSE> the response type
     * @param receiver the receiver to execute
     * @param context the signal context
     * @return a {@link Uni} that completes with the receiver's response
     */
    <SIGNAL, RESPONSE> Uni<RESPONSE> execute(Receiver<SIGNAL, RESPONSE> receiver, SignalContext<SIGNAL> context);

}
