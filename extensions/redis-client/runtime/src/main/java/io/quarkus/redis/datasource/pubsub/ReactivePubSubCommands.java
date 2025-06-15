package io.quarkus.redis.datasource.pubsub;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.quarkus.redis.datasource.ReactiveRedisCommands;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public interface ReactivePubSubCommands<V> extends ReactiveRedisCommands {

    /**
     * Publishes a message to a given channel
     *
     * @param channel
     *        the channel
     * @param message
     *        the message
     *
     * @return a Uni producing a {@code null} item once the message is sent, a failure otherwise.
     */
    Uni<Void> publish(String channel, V message);

    /**
     * Subscribes to a given channel.
     *
     * @param channel
     *        the channel
     * @param onMessage
     *        the message consumer. Be aware that this callback is invoked for each message sent to the given
     *        channel, and is invoked on the <strong>I/O thread</strong>. So, you must not block. Offload to a
     *        separate thread if needed.
     *
     * @return the subscriber object that lets you unsubscribe
     */
    Uni<ReactiveRedisSubscriber> subscribe(String channel, Consumer<V> onMessage);

    /**
     * Subscribes to a given pattern like {@code chan*l}.
     *
     * @param pattern
     *        the pattern
     * @param onMessage
     *        the message consumer. Be aware that this callback is invoked for each message sent to the channels
     *        matching the pattern, and is invoked on the <strong>I/O thread</strong>. So, you must not block.
     *        Offload to a separate thread if needed.
     *
     * @return the subscriber object that lets you unsubscribe
     */
    Uni<ReactiveRedisSubscriber> subscribeToPattern(String pattern, Consumer<V> onMessage);

    /**
     * Same as {@link #subscribeToPattern(String, Consumer)}, but instead of receiving only the message payload, it also
     * receives the name of the channel.
     *
     * @param pattern
     *        the pattern
     * @param onMessage
     *        the message consumer. Be aware that this callback is invoked for each message sent to the channels
     *        matching the pattern, and is invoked on the <strong>I/O thread</strong>. So, you must not block.
     *        Offload to a separate thread if needed. The first parameter is the name of the channel. The second
     *        parameter is the payload.
     *
     * @return the subscriber object that lets you unsubscribe
     */
    Uni<ReactiveRedisSubscriber> subscribeToPattern(String pattern, BiConsumer<String, V> onMessage);

    /**
     * Subscribes to the given patterns like {@code chan*l}.
     *
     * @param patterns
     *        the patterns
     * @param onMessage
     *        the message consumer. Be aware that this callback is invoked for each message sent to the channels
     *        matching the pattern, and is invoked on the <strong>I/O thread</strong>. So, you must not block.
     *        Offload to a separate thread if needed.
     *
     * @return the subscriber object that lets you unsubscribe
     */
    Uni<ReactiveRedisSubscriber> subscribeToPatterns(List<String> patterns, Consumer<V> onMessage);

    /**
     * Same as {@link #subscribeToPatterns(List, Consumer)}, but instead of only receiving the payload, it also receives
     * the channel name.
     *
     * @param patterns
     *        the patterns
     * @param onMessage
     *        the message consumer. Be aware that this callback is invoked for each message sent to the channels
     *        matching the pattern, and is invoked on the <strong>I/O thread</strong>. So, you must not block.
     *        Offload to a separate thread if needed. The first parameter is the channel name. The second one if the
     *        payload.
     *
     * @return the subscriber object that lets you unsubscribe
     */
    Uni<ReactiveRedisSubscriber> subscribeToPatterns(List<String> patterns, BiConsumer<String, V> onMessage);

    /**
     * Subscribes to the given channels.
     *
     * @param channels
     *        the channels
     * @param onMessage
     *        the message consumer. Be aware that this callback is invoked for each message sent to the given
     *        channels, and is invoked on the <strong>I/O thread</strong>. So, you must not block. Offload to a
     *        separate thread if needed.
     *
     * @return the subscriber object that lets you unsubscribe
     */
    Uni<ReactiveRedisSubscriber> subscribe(List<String> channels, Consumer<V> onMessage);

    /**
     * Same as {@link #subscribe(List, Consumer)}, but instead of just receiving the payload, it also receives the
     * channel name.
     *
     * @param channels
     *        the channels
     * @param onMessage
     *        the message consumer. Be aware that this callback is invoked for each message sent to the given
     *        channels, and is invoked on the <strong>I/O thread</strong>. So, you must not block. Offload to a
     *        separate thread if needed. The first parameter is the channel name. The second one if the payload.
     *
     * @return the subscriber object that lets you unsubscribe
     */
    Uni<ReactiveRedisSubscriber> subscribe(List<String> channels, BiConsumer<String, V> onMessage);

    /**
     * Subscribes to a given channel.
     *
     * @param channel
     *        the channel
     * @param onMessage
     *        the message consumer. Be aware that this callback is invoked for each message sent to the given
     *        channel, and is invoked on the <strong>I/O thread</strong>. So, you must not block. Offload to a
     *        separate thread if needed.
     * @param onEnd
     *        the end handler. Be aware that this callback is invoked on the <strong>I/O thread</strong>. So, you
     *        must not block. Offload to a separate thread if needed.
     * @param onException
     *        the exception handler. Be aware that this callback is invoked on the <strong>I/O thread</strong>. So,
     *        you must not block. Offload to a separate thread if needed.
     *
     * @return the subscriber object that lets you unsubscribe
     */
    Uni<ReactiveRedisSubscriber> subscribe(String channel, Consumer<V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException);

    /**
     * Subscribes to a given pattern like {@code chan*l}.
     *
     * @param pattern
     *        the pattern
     * @param onMessage
     *        the message consumer. Be aware that this callback is invoked for each message sent to the channels
     *        matching the pattern, and is invoked on the <strong>I/O thread</strong>. So, you must not block.
     *        Offload to a separate thread if needed.
     * @param onEnd
     *        the end handler. Be aware that this callback is invoked on the <strong>I/O thread</strong>. So, you
     *        must not block. Offload to a separate thread if needed.
     * @param onException
     *        the exception handler. Be aware that this callback is invoked on the <strong>I/O thread</strong>. So,
     *        you must not block. Offload to a separate thread if needed.
     *
     * @return the subscriber object that lets you unsubscribe
     */
    Uni<ReactiveRedisSubscriber> subscribeToPattern(String pattern, Consumer<V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException);

    /**
     * Same as {@link #subscribeToPatterns(List, Consumer, Runnable, Consumer)}, but also receives the channel name.
     *
     * @param pattern
     *        the pattern
     * @param onMessage
     *        the message consumer. Be aware that this callback is invoked for each message sent to the channels
     *        matching the pattern, and is invoked on the <strong>I/O thread</strong>. So, you must not block.
     *        Offload to a separate thread if needed. The first parameter is the name of the channel. The second
     *        parameter is the payload.
     * @param onEnd
     *        the end handler. Be aware that this callback is invoked on the <strong>I/O thread</strong>. So, you
     *        must not block. Offload to a separate thread if needed.
     * @param onException
     *        the exception handler. Be aware that this callback is invoked on the <strong>I/O thread</strong>. So,
     *        you must not block. Offload to a separate thread if needed.
     *
     * @return the subscriber object that lets you unsubscribe
     */
    Uni<ReactiveRedisSubscriber> subscribeToPattern(String pattern, BiConsumer<String, V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException);

    /**
     * Subscribes to the given patterns like {@code chan*l}.
     *
     * @param patterns
     *        the patterns
     * @param onMessage
     *        the message consumer. Be aware that this callback is invoked for each message sent to the channels
     *        matching the pattern, and is invoked on the <strong>I/O thread</strong>. So, you must not block.
     *        Offload to a separate thread if needed.
     * @param onEnd
     *        the end handler. Be aware that this callback is invoked on the <strong>I/O thread</strong>. So, you
     *        must not block. Offload to a separate thread if needed.
     * @param onException
     *        the exception handler. Be aware that this callback is invoked on the <strong>I/O thread</strong>. So,
     *        you must not block. Offload to a separate thread if needed.
     *
     * @return the subscriber object that lets you unsubscribe
     */
    Uni<ReactiveRedisSubscriber> subscribeToPatterns(List<String> patterns, Consumer<V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException);

    /**
     * Same as {@link #subscribeToPatterns(List, Consumer, Runnable, Consumer)}, but also receive the channel name.
     *
     * @param patterns
     *        the patterns
     * @param onMessage
     *        the message consumer. Be aware that this callback is invoked for each message sent to the channels
     *        matching the pattern, and is invoked on the <strong>I/O thread</strong>. So, you must not block.
     *        Offload to a separate thread if needed. The first parameter is the name of the channel. The second
     *        parameter is the payload.
     * @param onEnd
     *        the end handler. Be aware that this callback is invoked on the <strong>I/O thread</strong>. So, you
     *        must not block. Offload to a separate thread if needed.
     * @param onException
     *        the exception handler. Be aware that this callback is invoked on the <strong>I/O thread</strong>. So,
     *        you must not block. Offload to a separate thread if needed.
     *
     * @return the subscriber object that lets you unsubscribe
     */
    Uni<ReactiveRedisSubscriber> subscribeToPatterns(List<String> patterns, BiConsumer<String, V> onMessage,
            Runnable onEnd, Consumer<Throwable> onException);

    /**
     * Subscribes to the given channels.
     *
     * @param channels
     *        the channels
     * @param onMessage
     *        the message consumer. Be aware that this callback is invoked for each message sent to the given
     *        channels, and is invoked on the <strong>I/O thread</strong>. So, you must not block. Offload to a
     *        separate thread if needed.
     * @param onEnd
     *        the end handler. Be aware that this callback is invoked on the <strong>I/O thread</strong>. So, you
     *        must not block. Offload to a separate thread if needed.
     * @param onException
     *        the exception handler. Be aware that this callback is invoked on the <strong>I/O thread</strong>. So,
     *        you must not block. Offload to a separate thread if needed.
     *
     * @return the subscriber object that lets you unsubscribe
     */
    Uni<ReactiveRedisSubscriber> subscribe(List<String> channels, Consumer<V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException);

    /**
     * Same as {@link #subscribe(List, Consumer, Runnable, Consumer)} but also receives the channel name.
     *
     * @param channels
     *        the channels
     * @param onMessage
     *        the message consumer. Be aware that this callback is invoked for each message sent to the given
     *        channels, and is invoked on the <strong>I/O thread</strong>. So, you must not block. Offload to a
     *        separate thread if needed. The first parameter is the name of the channel. The second parameter is the
     *        payload.
     * @param onEnd
     *        the end handler. Be aware that this callback is invoked on the <strong>I/O thread</strong>. So, you
     *        must not block. Offload to a separate thread if needed.
     * @param onException
     *        the exception handler. Be aware that this callback is invoked on the <strong>I/O thread</strong>. So,
     *        you must not block. Offload to a separate thread if needed.
     *
     * @return the subscriber object that lets you unsubscribe
     */
    Uni<ReactiveRedisSubscriber> subscribe(List<String> channels, BiConsumer<String, V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException);

    /**
     * Subscribes to the given channels. This method returns a {@code Multi} emitting an item of type {@code V} for each
     * received message. This emission happens on the <strong>I/O thread</strong>, so you must not block. Use
     * {@code emitOn} to offload the processing to another thread.
     *
     * @param channels
     *        the channels
     *
     * @return the stream of message
     */
    Multi<V> subscribe(String... channels);

    /**
     * Same as {@link #subscribe(String...)}, but instead of receiving the message payload directly, it receives
     * instances of {@link RedisPubSubMessage} wrapping the payload and the channel on which the message has been sent.
     *
     * @param channels
     *        the channels
     *
     * @return the stream of message
     */
    Multi<RedisPubSubMessage<V>> subscribeAsMessages(String... channels);

    /**
     * Subscribes to the given patterns. This method returns a {@code Multi} emitting an item of type {@code V} for each
     * received message. This emission happens on the <strong>I/O thread</strong>, so you must not block. Use
     * {@code emitOn} to offload the processing to another thread.
     *
     * @param patterns
     *        the patterns
     *
     * @return the stream of message
     */
    Multi<V> subscribeToPatterns(String... patterns);

    /**
     * Same as {@link #subscribeToPatterns(String...)}, but instead of receiving only the message payload, it receives
     * instances of {@link RedisPubSubMessage} containing both the payload and the name of the channel.
     *
     * @param patterns
     *        the patterns
     *
     * @return the stream of message
     */
    Multi<RedisPubSubMessage<V>> subscribeAsMessagesToPatterns(String... patterns);

    /**
     * A redis subscriber
     */
    interface ReactiveRedisSubscriber {

        /**
         * Unsubscribes from the given channels/patterns
         *
         * @param channels
         *        the channels or patterns
         *
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
