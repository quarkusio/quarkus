package io.quarkus.reactivemessaging.http.runtime.serializers;

import io.vertx.core.buffer.Buffer;

/**
 * Serializer for Strings
 */
public class StringSerializer implements Serializer<String> {
    @Override
    public boolean handles(Object payload) {
        return payload instanceof String;
    }

    @Override
    public Buffer serialize(String payload) {
        return Buffer.buffer(payload);
    }
}
