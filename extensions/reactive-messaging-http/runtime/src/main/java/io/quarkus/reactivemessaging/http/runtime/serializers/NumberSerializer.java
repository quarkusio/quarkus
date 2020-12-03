package io.quarkus.reactivemessaging.http.runtime.serializers;

import io.vertx.core.buffer.Buffer;

/**
 * Serializer for numbers
 */
public class NumberSerializer implements Serializer<Number> {
    @Override
    public boolean handles(Object payload) {
        return payload instanceof Number;
    }

    @Override
    public Buffer serialize(Number payload) {
        return Buffer.buffer(payload.toString());
    }
}
