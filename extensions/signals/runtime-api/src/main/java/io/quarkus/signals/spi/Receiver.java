package io.quarkus.signals.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import io.quarkus.signals.Receivers;
import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.signals.SignalContext;
import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Uni;

/**
 * A receiver handles signals of a particular type. Receivers are typically declared as methods annotated with
 * {@link Receives} on a CDI bean, and discovered at build time. They can also be registered programmatically
 * via {@link Receivers}.
 *
 * <p>
 * A receiver method example:
 *
 * <pre>
 * void onOrderPlaced(&#064;Receives OrderPlaced order) {
 *     // handle the signal
 * }
 * </pre>
 *
 * <p>
 * The receiver method may also return a value, which is used as the response for
 * {@linkplain Signal#request(Object, Class) request-reply} emissions:
 *
 * <pre>
 * String onOrderPlaced(&#064;Receives OrderPlaced order) {
 *     return order.id();
 * }
 * </pre>
 *
 * @param <SIGNAL> the type of the signal object
 * @param <RESPONSE> the type of the response, or {@link Void} for fire-and-forget receivers
 * @see Receives
 * @see Signal
 * @see Receivers
 */
public interface Receiver<SIGNAL, RESPONSE> {

    /**
     * @return the received signal type
     */
    Type signalType();

    /**
     * The qualifiers are used during type-safe resolution together with the {@linkplain #signalType() signal type}.
     *
     * @return the set of qualifiers, never {@code null}
     */
    Set<Annotation> qualifiers();

    /**
     * The response type is used during type-safe resolution for request emissions. Only receivers whose
     * response type is assignable to the requested type are considered.
     *
     * @return the response type, or {@code null} for fire-and-forget receivers
     * @see Signal#request(Object, Class)
     */
    Type responseType();

    /**
     * Determines how the receiver is executed.
     *
     * @return the execution model
     * @see ExecutionModel
     */
    ExecutionModel executionModel();

    /**
     * Invoked when a matching signal is emitted. The {@link SignalContext} provides access to the signal object,
     * metadata, qualifiers, and emission type.
     *
     * @param context the signal context
     * @return a {@link Uni} that completes with the response, or with {@code null} for fire-and-forget receivers
     */
    @CheckReturnValue
    Uni<RESPONSE> notify(SignalContext<SIGNAL> context);

    /**
     * Determines the threading model used to execute a receiver.
     */
    enum ExecutionModel {

        /**
         * The receiver performs blocking operations and is offloaded to a worker thread.
         * This is the default for receiver methods with a blocking signature (i.e., not returning {@link Uni}).
         */
        BLOCKING,

        /**
         * The receiver is executed on a virtual thread.
         */
        VIRTUAL_THREAD,

        /**
         * The receiver performs non-blocking operations. When Vert.x is available, it is executed on the event loop.
         * This is the default for receiver methods returning {@link Uni}.
         */
        NON_BLOCKING

    }

}
