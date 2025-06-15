package io.quarkus.kafka.client.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonArray;

public class JsonArraySerializerTest {

    @Test
    void shouldSerializeEntity() {
        JsonArraySerializer serializer = new JsonArraySerializer();
        byte[] result = serializer.serialize("topic",
                new JsonArray(List.of(Map.of("id", 1, "name", "entity1"), Map.of("id", 2, "name", "entity2"))));
        assertNotNull(result);
        String actual = new String(result);
        assertThat(actual).contains("\"id\"").contains("\"name\"").contains("\"entity1\"").contains("1")
                .contains("\"entity2\"").contains("2");
    }

    @Test
    void shouldSerializeNullAsNull() {
        JsonArraySerializer serializer = new JsonArraySerializer();
        byte[] result = serializer.serialize("topic", null);
        assertNull(result);
    }
}
