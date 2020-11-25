package io.quarkus.reactivemessaging.http.runtime.serializers;

import io.vertx.core.buffer.Buffer;

/**
 * A no-op serializer for Buffers
 */
public class BufferSerializer implements Serializer<Buffer> {
    @Override
    public boolean handles(Object payload) {
        return payload instanceof Buffer;
    }

    @Override
    public Buffer serialize(Buffer payload) {
        return payload;
    }
}
