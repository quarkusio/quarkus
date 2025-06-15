package io.quarkus.redis.datasource.pubsub;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.quarkus.redis.datasource.RedisCommands;

/**
 * Allows executing Pub/Sub commands. See <a href="https://redis.io/docs/manual/pubsub/">the pub/sub documentation</a>.
 *
 * @param <V>
 *        the class of the exchanged messages.
 */
public interface PubSubCommands<V> extends RedisCommands {

    /**
     * Publishes a message to a given channel
     *
     * @param channel
     *        the channel
     * @param message
     *        the message
     */
    void publish(String channel, V message);

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
    RedisSubscriber subscribe(String channel, Consumer<V> onMessage);

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
    RedisSubscriber subscribeToPattern(String pattern, Consumer<V> onMessage);

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
    RedisSubscriber subscribeToPattern(String pattern, BiConsumer<String, V> onMessage);

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
    RedisSubscriber subscribeToPatterns(List<String> patterns, Consumer<V> onMessage);

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
    RedisSubscriber subscribeToPatterns(List<String> patterns, BiConsumer<String, V> onMessage);

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
    RedisSubscriber subscribe(List<String> channels, Consumer<V> onMessage);

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
    RedisSubscriber subscribe(String channel, Consumer<V> onMessage, Runnable onEnd, Consumer<Throwable> onException);

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
    RedisSubscriber subscribeToPattern(String pattern, Consumer<V> onMessage, Runnable onEnd,
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
    RedisSubscriber subscribeToPattern(String pattern, BiConsumer<String, V> onMessage, Runnable onEnd,
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
    RedisSubscriber subscribeToPatterns(List<String> patterns, Consumer<V> onMessage, Runnable onEnd,
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
    RedisSubscriber subscribeToPatterns(List<String> patterns, BiConsumer<String, V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException);

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
    RedisSubscriber subscribe(List<String> channels, Consumer<V> onMessage, Runnable onEnd,
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
    RedisSubscriber subscribe(List<String> channels, BiConsumer<String, V> onMessage, Runnable onEnd,
            Consumer<Throwable> onException);

    /**
     * A redis subscriber
     */
    interface RedisSubscriber {

        /**
         * Unsubscribes from the given channels/patterns
         *
         * @param channels
         *        the channels or patterns
         */
        void unsubscribe(String... channels);

        /**
         * Unsubscribes from all the subscribed channels/patterns
         */
        void unsubscribe();

    }
}
