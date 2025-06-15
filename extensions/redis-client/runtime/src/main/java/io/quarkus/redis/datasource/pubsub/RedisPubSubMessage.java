package io.quarkus.redis.datasource.pubsub;

/**
 * A structure encapsulating the Redis pub/sub payload and the channel on which the message was sent. This structure is
 * used when using {@link ReactivePubSubCommands#subscribeAsMessages(String...)} and
 * {@link ReactivePubSubCommands#subscribeAsMessagesToPatterns(String...)}
 *
 * @param <V>
 *        the type of payload
 */
public interface RedisPubSubMessage<V> {

    /**
     * @return the payload.
     */
    V getPayload();

    /**
     * @return the channel on which the message was sent.
     */
    String getChannel();

}
