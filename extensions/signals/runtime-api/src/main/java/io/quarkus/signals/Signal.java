package io.quarkus.signals;

import java.lang.annotation.Annotation;
import java.util.Map;

import jakarta.enterprise.util.TypeLiteral;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Uni;

/**
 * Allows the application to emit signals of a particular type and have them delivered to matching receivers.
 * <p>
 * A {@code Signal} may be injected:
 *
 * <pre>
 * &#064;Inject
 * Signal&lt;OrderPlaced&gt; orderPlaced;
 * </pre>
 *
 * Any combination of qualifiers may be specified at the injection point:
 *
 * <pre>
 * &#064;Inject
 * &#064;Urgent
 * Signal&lt;OrderPlaced&gt; urgentOrderPlaced;
 * </pre>
 *
 * <p>
 * Unlike CDI events, all matching receivers are always executed asynchronously. Blocking receivers are offloaded to a worker
 * thread, non-blocking receivers are executed on the Vert.x event loop (if available), and virtual thread receivers are
 * executed on a virtual thread.
 * <p>
 * A signal can be emitted in three ways:
 * <ul>
 * <li>{@link #publish(Object)} and {@link ReactiveEmission#publish(Object)} &mdash; delivers the signal to <em>all</em>
 * matching receivers (multicast).</li>
 * <li>{@link #send(Object)} and {@link ReactiveEmission#send(Object)} &mdash; delivers the signal to a <em>single</em>
 * matching receiver, selected in round-robin order (unicast).</li>
 * <li>{@link #request(Object, Class)} and {@link ReactiveEmission#request(Object, Class)} &mdash; delivers the signal to a
 * <em>single</em> matching receiver and returns the response (unicast, request-reply).</li>
 * </ul>
 *
 * For an injected {@code Signal}:
 * <ul>
 * <li>the <em>signal type</em> is the type parameter specified at the injection point, and</li>
 * <li>the <em>specified qualifiers</em> are the qualifiers specified at the injection point.</li>
 * </ul>
 *
 * <p>
 * When a signal is emitted, the container resolves the set of matching receivers using the signal type, qualifiers, and
 * &mdash; for request emissions &mdash; the response type. The resolution rules are inspired by the
 * <a href="https://jakarta.ee/specifications/cdi/4.1/jakarta-cdi-spec-4.1#observer_resolution">CDI observer resolution</a>
 * rules:
 * <ul>
 * <li>A receiver matches if the signal type is assignable to the received signal type.</li>
 * <li>A receiver matches only if the receiver's qualifiers are a <em>subset</em> of the signal's qualifiers.</li>
 * <li>For request emissions, a receiver matches only if its response type is assignable to the requested response type.</li>
 * </ul>
 * Every signal implicitly carries the {@code @Any} qualifier.
 * A {@code Signal} injection point with no qualifiers carries {@code @Default} (per the CDI specification).
 *
 * @param <T> the type of the signal object
 * @see Receives
 */
public interface Signal<T> {

    /**
     * Obtains a child {@code Signal} for the given additional required qualifiers.
     *
     * @param qualifiers the additional specified qualifiers
     * @return the child {@code Signal}
     */
    Signal<T> select(Annotation... qualifiers);

    /**
     * Obtains a child {@code Signal} for the given required type and additional required qualifiers.
     *
     * @param <U> the specified type
     * @param subtype a {@link Class} representing the required type
     * @param qualifiers the additional specified qualifiers
     * @return the child {@code Signal}
     */
    <U extends T> Signal<U> select(Class<U> subtype, Annotation... qualifiers);

    /**
     * Obtains a child {@code Signal} for the given required type and additional required qualifiers.
     *
     * @param <U> the specified type
     * @param subtype a {@link TypeLiteral} representing the required type
     * @param qualifiers the additional specified qualifiers
     * @return the child {@code Signal}
     */
    <U extends T> Signal<U> select(TypeLiteral<U> subtype, Annotation... qualifiers);

    /**
     * Obtains a child {@code Signal} with the given metadata entry, replacing any previously added entry for the given key
     * entries.
     * <p>
     * <p>
     * The metadata entries will be used for all emissions from the returned signal instance.
     *
     * @param key
     * @param value
     * @return the child {@code Signal}
     */
    Signal<T> putMetadata(String key, Object value);

    /**
     * Obtains a child {@code Signal} with the given metadata, replacing any previously added metadata entries.
     * <p>
     * The metadata entries will be used for all emissions from the returned signal instance.
     *
     * @param metadata
     * @return the child {@code Signal}
     */
    Signal<T> setMetadata(Map<String, Object> metadata);

    /**
     * Sends a signal to <em>all</em> receivers matching the specified signal type and qualifiers (multicast, fire-and-forget).
     * <p>
     * All receivers are executed asynchronously.
     * If no receiver matches, the method succeeds silently.
     * <p>
     * To perform additional logic when all receivers have completed, use {@link #reactive()} and then
     * {@link ReactiveEmission#publish(Object)}.
     *
     * @param signal the signal object
     * @see ReactiveEmission#publish(Object)
     * @see Receives
     */
    void publish(T signal);

    /**
     * Sends a signal to a <em>single</em> receiver matching the specified signal type and qualifiers
     * (unicast, fire-and-forget).
     * <p>
     * If multiple receivers match, one is selected in round-robin order.
     * If no receiver matches, the method succeeds silently.
     * <p>
     * To perform additional logic when the receiver has completed, use {@link #reactive()} and then
     * {@link ReactiveEmission#send(Object)}.
     *
     * @param signal the signal object
     * @see ReactiveEmission#send(Object)
     * @see Receives
     */
    void send(T signal);

    /**
     * Sends a signal to a <em>single</em> receiver matching the specified signal type, response type and qualifiers, and
     * blocks the current thread until the response is available (unicast, request-reply).
     * <p>
     * If multiple receivers match, one is selected in round-robin order.
     * If no receiver matches, returns {@code null}.
     * <p>
     * For a non-blocking variant, use {@link #reactive()} and then {@link ReactiveEmission#request(Object, Class)}.
     *
     * @param <R> the response type
     * @param signal the signal object
     * @param responseType the expected response type
     * @return the receiver's response, or {@code null} if no receiver matches
     * @see ReactiveEmission#request(Object, Class)
     * @see Receives
     */
    <R> R request(T signal, Class<R> responseType);

    /**
     * Sends a signal to a <em>single</em> receiver matching the specified signal type, response type and qualifiers, and
     * blocks the current thread until the response is available (unicast, request-reply).
     * <p>
     * If multiple receivers match, one is selected in round-robin order.
     * If no receiver matches, returns {@code null}.
     * <p>
     * For a non-blocking variant, use {@link #reactive()} and then {@link ReactiveEmission#request(Object, TypeLiteral)}.
     *
     * @param <R> the response type
     * @param signal the signal object
     * @param responseType a {@link TypeLiteral} representing the expected response type
     * @return the receiver's response, or {@code null} if no receiver matches
     * @see ReactiveEmission#request(Object, TypeLiteral)
     * @see Receives
     */
    <R> R request(T signal, TypeLiteral<R> responseType);

    /**
     * Returns a {@link ReactiveEmission} that provides reactive variants of the emission methods.
     *
     * @return the reactive emission
     */
    ReactiveEmission<T> reactive();

    /**
     * Provides reactive variants of the signal emission methods.
     * <p>
     * Each method returns a {@link Uni} that, when subscribed, emits the signal and completes when the receiver(s) finish.
     * The returned {@code Uni} is <em>lazy</em>: no signal is emitted until the {@code Uni} is subscribed.
     * Each subscription triggers a new, independent emission.
     *
     * @param <T> the type of the signal object
     */
    interface ReactiveEmission<T> {

        /**
         * Sends a signal to <em>all</em> receivers matching the specified signal type and qualifiers (multicast).
         * <p>
         * All receivers are executed asynchronously.
         * If no receiver matches, the returned {@link Uni} completes with {@code null}.
         *
         * @param signal the signal object
         * @return a {@link Uni} that completes when all receivers are executed
         * @see Signal#publish(Object)
         */
        @CheckReturnValue
        Uni<Void> publish(T signal);

        /**
         * Sends a signal to a <em>single</em> receiver matching the specified signal type and qualifiers
         * (unicast).
         * <p>
         * If multiple receivers match, one is selected in round-robin order.
         * If no receiver matches, the returned {@link Uni} completes with {@code null}.
         *
         * @param signal the signal object
         * @return a {@link Uni} that completes when a receiver is executed
         * @see Signal#send(Object)
         */
        @CheckReturnValue
        Uni<Void> send(T signal);

        /**
         * Sends a signal to a <em>single</em> receiver matching the specified signal type, response type and qualifiers, and
         * returns the response (unicast, request-reply).
         * <p>
         * If multiple receivers match, one is selected in round-robin order.
         * If no receiver matches, the returned {@link Uni} completes with {@code null}.
         *
         * @param <R> the response type
         * @param signal the signal object
         * @param responseType the expected response type
         * @return a {@link Uni} that completes with the receiver's response
         * @see Signal#request(Object, Class)
         * @see Receives
         */
        @CheckReturnValue
        <R> Uni<R> request(T signal, Class<R> responseType);

        /**
         * Sends a signal to a <em>single</em> receiver matching the specified signal type, response type and qualifiers, and
         * returns the response (unicast, request-reply).
         * <p>
         * If multiple receivers match, one is selected in round-robin order.
         * If no receiver matches, the returned {@link Uni} completes with {@code null}.
         *
         * @param <R> the response type
         * @param signal the signal object
         * @param responseType a {@link TypeLiteral} representing the expected response type
         * @return a {@link Uni} that completes with the receiver's response
         * @see Signal#request(Object, TypeLiteral)
         * @see Receives
         */
        @CheckReturnValue
        <R> Uni<R> request(T signal, TypeLiteral<R> responseType);

    }

}
