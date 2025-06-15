package io.quarkus.kafka.client.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;

public class JsonArrayDeserializerTest {
    @Test
    void shouldDeserializeEntity() {
        JsonArray expected = new JsonArray(
                List.of(Map.of("id", 1, "name", "entity1"), Map.of("id", 2, "name", "entity2")));
        JsonArrayDeserializer deserializer = new JsonArrayDeserializer();
        String actualString = "[" + "{\"id\":1,\"name\":\"entity1\"}," + "{\"id\":2,\"name\":\"entity2\"}" + "]";
        JsonArray actual = deserializer.deserialize("topic", actualString.getBytes());
        assertNotNull(actual);
        assertEquals(expected, actual);
    }

    @Test
    void shouldThrowDecodeExceptionOnDeserializeNull() {
        JsonArrayDeserializer deserializer = new JsonArrayDeserializer();
        assertThrows(DecodeException.class, () -> deserializer.deserialize("topic", "null".getBytes()));
    }

    @Test
    void shouldDeserializeNullAsNull() {
        JsonArrayDeserializer deserializer = new JsonArrayDeserializer();
        JsonArray actual = deserializer.deserialize("topic", null);
        assertNull(actual);
    }
}
