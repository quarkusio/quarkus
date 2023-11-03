package io.quarkus.kafka.client.serialization;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import io.vertx.core.buffer.Buffer;

public class BufferSerializerTest {
    @Test
    void shouldSerializeEntity() {
        BufferSerializer serializer = new BufferSerializer();
        byte[] actual = serializer.serialize("topic", Buffer.buffer("some-bytes"));
        assertNotNull(actual);
    }

    @Test
    void shouldSerializeNullAsNull() {
        BufferSerializer serializer = new BufferSerializer();
        byte[] result = serializer.serialize("topic", null);
        assertNull(result);
    }
}
