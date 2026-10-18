package io.quarkus.redis.datasource.stream;

import java.time.Duration;
import java.util.Map;

/**
 * Represents a message received from a stream
 *
 * @param <K> the type of the key
 * @param <F> the field type for the payload
 * @param <V> the value type for the payload
 */
public class StreamMessage<K, F, V> {

    private final K stream;
    private final String id;
    private final Map<F, V> payload;
    private final Duration durationSinceLastDelivery;
    private final int deliveryCount;

    public StreamMessage(K stream, String id, Map<F, V> payload) {
        this(stream, id, payload, null, 0);
    }

    public StreamMessage(K stream, String id, Map<F, V> payload, Duration durationSinceLastDelivery, int deliveryCount) {
        this.stream = stream;
        this.id = id;
        this.payload = payload;
        this.durationSinceLastDelivery = durationSinceLastDelivery;
        this.deliveryCount = deliveryCount;
    }

    /**
     * @return the key of the stream from which the message has been received.
     */
    public K key() {
        return this.stream;
    }

    /**
     * @return the stream id, i.e. the id of the message in the stream.
     */
    public String id() {
        return this.id;
    }

    /**
     * @return the payload of the message
     */
    public Map<F, V> payload() {
        return this.payload;
    }

    /**
     * @return the duration since this message was last delivered to a consumer,
     *         or {@code null} if this message was not reclaimed via the CLAIM option.
     *         Only present when the XREADGROUP CLAIM option is used and this message was a pending entry.
     */
    public Duration durationSinceLastDelivery() {
        return this.durationSinceLastDelivery;
    }

    /**
     * @return the number of times this message has been delivered, or {@code 0} if this message was not reclaimed
     *         via the CLAIM option.
     *         Only present when the XREADGROUP CLAIM option is used and this message was a pending entry.
     */
    public int deliveryCount() {
        return this.deliveryCount;
    }
}
