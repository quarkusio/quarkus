package io.quarkus.redis.datasource.stream;

import java.util.Map;

/**
 * Represents a message received from a stream
 *
 * @param <K>
 *        the type of the key
 * @param <F>
 *        the field type for the payload
 * @param <V>
 *        the value type for the payload
 */
public class StreamMessage<K, F, V> {

    private final K stream;
    private final String id;
    private final Map<F, V> payload;

    public StreamMessage(K stream, String id, Map<F, V> payload) {
        this.stream = stream;
        this.id = id;
        this.payload = payload;
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
}
