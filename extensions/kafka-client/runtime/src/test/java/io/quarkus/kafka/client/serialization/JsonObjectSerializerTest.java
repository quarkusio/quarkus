package io.quarkus.kafka.client.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

public class JsonObjectSerializerTest {

    @Test
    void shouldSerializeJsonObject() {
        JsonObjectSerializer serializer = new JsonObjectSerializer();
        byte[] result = serializer.serialize("topic", new JsonObject(Map.of("id", 1, "name", "entity1")));
        assertNotNull(result);
        String actual = new String(result);
        assertThat(actual).contains("\"id\"").contains("\"name\"").contains("\"entity1\"").contains("1");
    }

    @Test
    void shouldSerializeNullAsNull() {
        JsonObjectSerializer serializer = new JsonObjectSerializer();
        byte[] result = serializer.serialize("topic", null);
        assertNull(result);
    }
}
