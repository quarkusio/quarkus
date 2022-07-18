package io.quarkus.kafka.client.serialization;

import org.apache.kafka.common.serialization.Serializer;

import io.vertx.core.buffer.Buffer;

/**
 * Kafka serializer for raw bytes in a buffer
 */
public class BufferSerializer implements Serializer<Buffer> {

    @Override
    public byte[] serialize(String topic, Buffer data) {
        if (data == null)
            return null;

        return data.getBytes();
    }
}
