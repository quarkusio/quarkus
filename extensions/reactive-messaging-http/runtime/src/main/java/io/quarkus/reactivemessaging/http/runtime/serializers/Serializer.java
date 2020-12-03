package io.quarkus.reactivemessaging.http.runtime.serializers;

import io.vertx.core.buffer.Buffer;

/**
 * Reactive http connector serializer.
 * Serializes given payload to a {@link Buffer}
 *
 * The serializers are sorted by Priority.
 * If more than one says that it {@link Serializer#handles(Object)} the ones with the highest priority is used
 *
 * @param <PayloadType> type of the payload to serialize
 */
public interface Serializer<PayloadType> {

    /**
     * default serializer priority
     */
    int DEFAULT_PRIORITY = 0;

    /**
     * if the serializer can handle given payload
     *
     * @param payload the payload
     * @return true iff the seralizer can handle the payload
     */
    boolean handles(Object payload);

    /**
     * serialize the payload
     * 
     * @param payload object to serialize
     * @return a buffer with serialized payload
     */
    Buffer serialize(PayloadType payload);

    /**
     * From serializers that can handle a specific payload, the one with the higher priority is used.
     * 
     * @return the priority of the serializer
     */
    default int getPriority() {
        return DEFAULT_PRIORITY;
    }
}
