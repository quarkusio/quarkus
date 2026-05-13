package io.quarkus.signals;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.util.TypeLiteral;

import io.smallrye.common.annotation.Experimental;
import io.smallrye.mutiny.Uni;

/**
 * Allows programmatic registration of receivers at runtime.
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
     * Resolves the receivers matching the given signal type and qualifiers.
     * <p>
     * The resolution rules are the same as for signal emissions.
     * If no qualifiers are given, the {@linkplain jakarta.enterprise.inject.Default default qualifier} is assumed.
     *
     * @param signalType the signal type
     * @param qualifiers the qualifiers
     * @return the list of matching receivers, never {@code null}
     * @see Signal
     */
    List<ReceiverInfo> resolveReceivers(Class<?> signalType, Annotation... qualifiers);

    /**
     * Resolves the receivers matching the given signal type and qualifiers.
     * <p>
     * The resolution rules are the same as for signal emissions.
     * If no qualifiers are given, the {@linkplain jakarta.enterprise.inject.Default default qualifier} is assumed.
     *
     * @param signalType the signal type
     * @param qualifiers the qualifiers
     * @return the list of matching receivers, never {@code null}
     * @see Signal
     */
    List<ReceiverInfo> resolveReceivers(TypeLiteral<?> signalType, Annotation... qualifiers);

    /**
     * Read-only information about a resolved receiver.
     */
    interface ReceiverInfo {

        /**
         * @return the received signal type
         */
        Type signalType();

        /**
         * @return the set of qualifiers, never {@code null}
         */
        Set<Annotation> qualifiers();

        /**
         * @return the response type
         */
        Type responseType();

        /**
         * @return the execution model
         */
        ExecutionModel executionModel();

        /**
         * @return the kind of the receiver
         */
        ReceiverKind kind();

    }

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
            return setResponseType(void.class).notify(new Function<SignalContext<SIGNAL>, Uni<Void>>() {
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

    /**
     * Determines the threading model used to execute a receiver.
     */
    enum ExecutionModel {

        /**
         * The receiver performs blocking operations and is offloaded to a worker thread.
         * This is the default for receiver methods with a blocking signature (i.e., not returning {@link Uni}
         * or {@link java.util.concurrent.CompletionStage}).
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

    /**
     * The kind of receiver definition.
     */
    enum ReceiverKind {

        /**
         * A declarative receiver defined as a method annotated with {@link Receives}.
         */
        DECLARATIVE,

        /**
         * A programmatic receiver registered via {@link Receivers#newReceiver(Class)}.
         */
        PROGRAMMATIC

    }

}
