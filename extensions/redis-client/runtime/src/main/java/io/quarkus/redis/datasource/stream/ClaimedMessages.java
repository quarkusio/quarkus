package io.quarkus.redis.datasource.stream;

import java.util.List;

/**
 * Represents claimed messages
 *
 * @param <K>
 *        the type of the key
 * @param <F>
 *        the field type for the payload
 * @param <V>
 *        the value type for the payload
 */
public class ClaimedMessages<K, F, V> {
    private final String id;
    private final List<StreamMessage<K, F, V>> messages;

    public ClaimedMessages(String id, List<StreamMessage<K, F, V>> messages) {
        this.id = id;
        this.messages = messages;
    }

    public String getId() {
        return this.id;
    }

    public List<StreamMessage<K, F, V>> getMessages() {
        return this.messages;
    }
}
