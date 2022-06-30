package io.quarkus.redis.datasource.api.pubsub;

import java.util.List;
import java.util.function.Consumer;

/**
 * Allows executing Pub/Sub commands.
 * See <a href="https://redis.io/docs/manual/pubsub/">the pub/sub documentation</a>.
 *
 * @param <V> the class of the exchanged messages.
 */
public interface PubSubCommands<V> {

    /**
     * Publishes a message to a given channel
     *
     * @param channel the channel
     * @param message the message
     */
    void publish(String channel, V message);

    /**
     * Subscribes to a given channel.
     *
     * @param channel the channel
     * @param onMessage the message consumer. Be aware that this callback is invoked for each message sent to the given
     *        channel, and is invoked on the <strong>I/O thread</strong>. So, you must not block. Offload to
     *        a separate thread if needed.
     * @return the subscriber object that lets you unsubscribe
     */
    RedisSubscriber subscribe(String channel, Consumer<V> onMessage);

    /**
     * Subscribes to a given pattern like {@code chan*l}.
     *
     * @param pattern the pattern
     * @param onMessage the message consumer. Be aware that this callback is invoked for each message sent to the
     *        channels matching the pattern, and is invoked on the <strong>I/O thread</strong>. So, you must
     *        not block. Offload to a separate thread if needed.
     * @return the subscriber object that lets you unsubscribe
     */
    RedisSubscriber subscribeToPattern(String pattern, Consumer<V> onMessage);

    /**
     * Subscribes to the given patterns like {@code chan*l}.
     *
     * @param patterns the patterns
     * @param onMessage the message consumer. Be aware that this callback is invoked for each message sent to the
     *        channels matching the pattern, and is invoked on the <strong>I/O thread</strong>. So, you must
     *        not block. Offload to a separate thread if needed.
     * @return the subscriber object that lets you unsubscribe
     */
    RedisSubscriber subscribeToPatterns(List<String> patterns, Consumer<V> onMessage);

    /**
     * Subscribes to the given channels.
     *
     * @param channels the channels
     * @param onMessage the message consumer. Be aware that this callback is invoked for each message sent to the given
     *        channels, and is invoked on the <strong>I/O thread</strong>. So, you must not block. Offload to
     *        a separate thread if needed.
     * @return the subscriber object that lets you unsubscribe
     */
    RedisSubscriber subscribe(List<String> channels, Consumer<V> onMessage);

    /**
     * A redis subscriber
     */
    interface RedisSubscriber {

        /**
         * Unsubscribes from the given channels/patterns
         *
         * @param channels the channels or patterns
         */
        void unsubscribe(String... channels);

        /**
         * Unsubscribes from all the subscribed channels/patterns
         */
        void unsubscribe();

    }
}
