package io.quarkus.redis.datasource.pubsub;

import java.util.List;
import java.util.function.Consumer;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public interface ReactivePubSubCommands<V> {

    /**
     * Publishes a message to a given channel
     *
     * @param channel the channel
     * @param message the message
     * @return a Uni producing a {@code null} item once the message is sent, a failure otherwise.
     */
    Uni<Void> publish(String channel, V message);

    /**
     * Subscribes to a given channel.
     *
     * @param channel the channel
     * @param onMessage the message consumer. Be aware that this callback is invoked for each message sent to the given
     *        channel, and is invoked on the <strong>I/O thread</strong>. So, you must not block. Offload to
     *        a separate thread if needed.
     * @return the subscriber object that lets you unsubscribe
     */
    Uni<ReactiveRedisSubscriber> subscribe(String channel, Consumer<V> onMessage);

    /**
     * Subscribes to a given pattern like {@code chan*l}.
     *
     * @param pattern the pattern
     * @param onMessage the message consumer. Be aware that this callback is invoked for each message sent to the
     *        channels matching the pattern, and is invoked on the <strong>I/O thread</strong>. So, you must
     *        not block. Offload to a separate thread if needed.
     * @return the subscriber object that lets you unsubscribe
     */
    Uni<ReactiveRedisSubscriber> subscribeToPattern(String pattern, Consumer<V> onMessage);

    /**
     * Subscribes to the given patterns like {@code chan*l}.
     *
     * @param patterns the patterns
     * @param onMessage the message consumer. Be aware that this callback is invoked for each message sent to the
     *        channels matching the pattern, and is invoked on the <strong>I/O thread</strong>. So, you must
     *        not block. Offload to a separate thread if needed.
     * @return the subscriber object that lets you unsubscribe
     */
    Uni<ReactiveRedisSubscriber> subscribeToPatterns(List<String> patterns, Consumer<V> onMessage);

    /**
     * Subscribes to the given channels.
     *
     * @param channels the channels
     * @param onMessage the message consumer. Be aware that this callback is invoked for each message sent to the given
     *        channels, and is invoked on the <strong>I/O thread</strong>. So, you must not block. Offload to
     *        a separate thread if needed.
     * @return the subscriber object that lets you unsubscribe
     */
    Uni<ReactiveRedisSubscriber> subscribe(List<String> channels, Consumer<V> onMessage);

    /**
     * Subscribes to the given channels.
     * This method returns a {@code Multi} emitting an item of type {@code V} for each received message.
     * This emission happens on the <strong>I/O thread</strong>, so you must not block. Use {@code emitOn} to offload
     * the processing to another thread.
     *
     * @param channels the channels
     * @return the stream of message
     */
    Multi<V> subscribe(String... channels);

    /**
     * Subscribes to the given patterns.
     * This method returns a {@code Multi} emitting an item of type {@code V} for each received message.
     * This emission happens on the <strong>I/O thread</strong>, so you must not block. Use {@code emitOn} to offload
     * the processing to another thread.
     *
     * @param channels the channels
     * @return the stream of message
     */
    Multi<V> subscribeToPatterns(String... patterns);

    /**
     * A redis subscriber
     */
    interface ReactiveRedisSubscriber {

        /**
         * Unsubscribes from the given channels/patterns
         *
         * @param channels the channels or patterns
         * @return a Uni emitting {@code null} when the operation completes successfully, a failure otherwise.
         */
        Uni<Void> unsubscribe(String... channels);

        /**
         * Unsubscribes from all the subscribed channels/patterns
         *
         * @return a Uni emitting {@code null} when the operation completes successfully, a failure otherwise.
         */
        Uni<Void> unsubscribe();

    }
}
