package io.quarkus.signals;

import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.util.TypeLiteral;

import io.quarkus.signals.spi.Receiver;
import io.quarkus.signals.spi.Receiver.ExecutionModel;
import io.smallrye.common.annotation.Experimental;
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
@Experimental("This API is experimental and may change in the future")
public interface Receivers {

    /**
     * Returns a builder of a new receiver of the given {@code signalType}.
     *
     * @param <SIGNAL> the received signal type
     * @param signalType the received signal type
     * @return a new receiver builder
     */
    <SIGNAL> ReceiverDefinition<SIGNAL, Void> newReceiver(Class<SIGNAL> signalType);

    /**
     * Returns a builder of a new receiver of the given {@code signalType}.
     *
     * @param <SIGNAL> the received signal type
     * @param signalType the received signal type
     * @return a new receiver builder
     */
    <SIGNAL> ReceiverDefinition<SIGNAL, Void> newReceiver(TypeLiteral<SIGNAL> signalType);

    /**
     * A builder for creating {@link Receiver} instances programmatically.
     *
     * @param <SIGNAL> the signal type
     * @param <RESPONSE> the response type; {@code Void} by default, set by {@code setResponseType()}
     * @see Receivers#newReceiver(Class)
     */
    interface ReceiverDefinition<SIGNAL, RESPONSE> {

        /**
         * Sets the qualifiers of the built receiver.
         * By default, no qualifiers are present, which results in presence of a single {@code @Default} qualifier.
         *
         * @param qualifiers the set of qualifiers
         * @return self
         */
        ReceiverDefinition<SIGNAL, RESPONSE> setQualifiers(Annotation... qualifiers);

        /**
         * Sets the execution model of the receiver.
         * By default, {@link ExecutionModel#BLOCKING} is used.
         *
         * @param executionModel the execution model
         * @return self
         */
        ReceiverDefinition<SIGNAL, RESPONSE> setExecutionModel(ExecutionModel executionModel);

        /**
         * Sets the response type of the receiver.
         * By default, {@code Void} is used.
         *
         * @param <R> the response type
         * @param responseType the response type
         * @return self
         */
        <R> ReceiverDefinition<SIGNAL, R> setResponseType(Class<R> responseType);

        /**
         * Sets the response type of the receiver.
         * By default, {@code Void} is used.
         *
         * @param <R> the response type
         * @param responseType the response type
         * @return self
         */
        <R> ReceiverDefinition<SIGNAL, R> setResponseType(TypeLiteral<R> responseType);

        /**
         * Registers a new receiver.
         * When registered through this method, the receiver always has a response type of {@code Void}.
         * <p>
         * Registration is not performed atomically; concurrent emissions may or may not see the receiver while this method
         * executes. When the method returns, the receiver is fully registered.
         *
         * @param callback a consumer of the signal
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
         * <p>
         * Registration is not performed atomically; concurrent emissions may or may not see the receiver while this method
         * executes. When the method returns, the receiver is fully registered.
         *
         * @param callback a function from the signal to the response
         * @return a new receiver
         */
        Registration notify(Function<SignalContext<SIGNAL>, Uni<RESPONSE>> callback);

    }

    /**
     * A handle returned by {@link ReceiverDefinition#notify(Function)} that can be used to unregister the receiver.
     */
    interface Registration {

        /**
         * Unregisters the receiver.
         * <p>
         * Unregistration is not performed atomically; concurrent emissions may or may not see the receiver while this
         * method executes. When the method returns, the receiver is fully unregistered.
         */
        void unregister();

    }

}
