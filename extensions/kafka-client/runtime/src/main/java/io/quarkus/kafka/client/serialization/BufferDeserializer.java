package io.quarkus.kafka.client.serialization;

import org.apache.kafka.common.serialization.Deserializer;

import io.vertx.core.buffer.Buffer;

/**
 * Kafka deserializer for raw bytes in a buffer
 */
public class BufferDeserializer implements Deserializer<Buffer> {

    @Override
    public Buffer deserialize(String topic, byte[] data) {
        if (data == null)
            return null;

        return Buffer.buffer(data);
    }

}
