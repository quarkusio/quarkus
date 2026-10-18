package io.quarkus.kafka.client.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;

public class JsonObjectDeserializerTest {

    @Test
    void shouldDeserializeEntity() {
        JsonObject expected = new JsonObject(Map.of("id", 1, "name", "entity1"));
        JsonObjectDeserializer deserializer = new JsonObjectDeserializer();
        JsonObject actual = deserializer.deserialize("topic", "{\"id\":1,\"name\":\"entity1\"}".getBytes());
        assertNotNull(actual);
        assertEquals(expected, actual);
    }

    @Test
    void shouldThrowDecodeExceptionOnDeserializeNull() {
        JsonObjectDeserializer deserializer = new JsonObjectDeserializer();
        assertThrows(DecodeException.class, () -> deserializer.deserialize("topic", "null".getBytes()));
    }

    @Test
    void shouldDeserializeNullAsNull() {
        JsonObjectDeserializer deserializer = new JsonObjectDeserializer();
        JsonObject actual = deserializer.deserialize("topic", null);
        assertNull(actual);
    }
}
