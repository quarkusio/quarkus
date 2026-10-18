package io.quarkus.signals.spi;

import io.quarkus.signals.Receivers;
import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.signals.SignalContext;
import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.common.annotation.Experimental;
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
@Experimental("This API is experimental and may change in the future")
public interface Receiver<SIGNAL, RESPONSE> extends Receivers.ReceiverInfo {

    /**
     * Invoked when a matching signal is emitted. The {@link SignalContext} provides access to the signal object,
     * metadata, qualifiers, and emission type.
     *
     * @param context the signal context
     * @return a {@link Uni} that completes with the response, or with {@code null} for fire-and-forget receivers
     */
    @CheckReturnValue
    Uni<RESPONSE> notify(SignalContext<SIGNAL> context);

}
