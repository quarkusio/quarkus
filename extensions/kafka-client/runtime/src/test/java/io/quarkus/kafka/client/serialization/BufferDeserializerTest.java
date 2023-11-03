package io.quarkus.kafka.client.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import io.vertx.core.buffer.Buffer;

public class BufferDeserializerTest {

    @Test
    void shouldDeserializeEntity() {
        BufferDeserializer deserializer = new BufferDeserializer();
        Buffer actual = deserializer.deserialize("topic", "some-bytes".getBytes());
        assertEquals(Buffer.buffer("some-bytes"), actual);
    }

    @Test
    void shouldDeserializeNullAsNullString() {
        BufferDeserializer deserializer = new BufferDeserializer();
        Buffer actual = deserializer.deserialize("topic", "null".getBytes());
        assertEquals(Buffer.buffer("null"), actual);
    }

    @Test
    void shouldDeserializeNullAsNull() {
        BufferDeserializer deserializer = new BufferDeserializer();
        Buffer actual = deserializer.deserialize("topic", null);
        assertNull(actual);
    }
}
