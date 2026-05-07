package io.quarkus.signals;

import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.util.TypeLiteral;

import io.quarkus.signals.spi.Receiver;
import io.quarkus.signals.spi.Receiver.ExecutionModel;
import io.smallrye.mutiny.Uni;

/**
 * Allows programmatic registration of {@link Receiver} instances at runtime.
 * <p>
 * An instance can be injected as a CDI bean; the implementation is provided by the extension.
 *
 * <pre>
 * &#064;Inject
 * Receivers receivers;
 *
 * void register() {
 *     Registration reg = receivers.newReceiver(MySignal.class)
 *             .notify(ctx -&gt; System.out.println(ctx.signal()));
 *     // later...
 *     reg.unregister();
 * }
 * </pre>
 *
 * @see Signal
 */
public interface Receivers {

    /**
     *
     * @param <SIGNAL>
     * @param signalType the received signal type
     * @return a new definition
     */
    <SIGNAL> ReceiverDefinition<SIGNAL, Void> newReceiver(Class<SIGNAL> signalType);

    /**
     *
     * @param <SIGNAL>
     * @param signalType the received signal type
     * @return a new definition
     */
    <SIGNAL> ReceiverDefinition<SIGNAL, Void> newReceiver(TypeLiteral<SIGNAL> signalType);

    /**
     * A builder for creating {@link Receiver} instances programmatically.
     *
     * @param <SIGNAL> the signal type
     * @see Receivers#builder(Class)
     */
    interface ReceiverDefinition<SIGNAL, RESPONSE> {

        /**
         * @param qualifiers
         * @return self
         */
        ReceiverDefinition<SIGNAL, RESPONSE> setQualifiers(Annotation... qualifiers);

        /**
         * @param executionModel
         * @return self
         */
        ReceiverDefinition<SIGNAL, RESPONSE> setExecutionModel(ExecutionModel executionModel);

        /**
         *
         * @param <R>
         * @param responseType
         * @return self
         */
        <R> ReceiverDefinition<SIGNAL, R> setResponseType(Class<R> responseType);

        /**
         *
         * @param <R>
         * @param responseType
         * @return self
         */
        <R> ReceiverDefinition<SIGNAL, R> setResponseType(TypeLiteral<R> responseType);

        /**
         * Registers a new receiver.
         *
         * @param callback
         * @return a new receiver
         */
        default Registration notify(Consumer<SignalContext<SIGNAL>> callback) {
            Objects.requireNonNull(callback);
            return setResponseType(Void.class).notify(new Function<SignalContext<SIGNAL>, Uni<Void>>() {
                @Override
                public Uni<Void> apply(SignalContext<SIGNAL> ctx) {
                    try {
                        callback.accept(ctx);
                        return Uni.createFrom().voidItem();
                    } catch (Exception e) {
                        return Uni.createFrom().failure(e);
                    }
                }
            });
        }

        /**
         * Registers a new receiver.
         *
         * @param callback
         * @return a new receiver
         */
        Registration notify(Function<SignalContext<SIGNAL>, Uni<RESPONSE>> callback);

    }

    /**
     * A handle returned by {@link #register(Receiver)} that can be used to unregister the receiver.
     */
    interface Registration {

        /**
         * Unregisters the receiver.
         */
        void unregister();

    }

}
