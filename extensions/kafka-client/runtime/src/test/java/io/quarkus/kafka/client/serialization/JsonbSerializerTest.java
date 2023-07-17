package io.quarkus.kafka.client.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class JsonbSerializerTest {

    @Test
    void shouldSerializeEntity() {
        JsonbSerializer<MyEntity> serializer = new JsonbSerializer<>();
        MyEntity entity = new MyEntity(1, "entity1");
        byte[] result = serializer.serialize("topic", entity);
        assertNotNull(result);
        assertEquals("{\"id\":1,\"name\":\"entity1\"}", new String(result));
    }

    @Test
    void shouldSerializeListOfEntities() {
        JsonbSerializer<List<MyEntity>> serializer = new JsonbSerializer<>();
        MyEntity entity1 = new MyEntity(1, "entity1");
        MyEntity entity2 = new MyEntity(2, "entity2");
        byte[] result = serializer.serialize("topic", List.of(entity1, entity2));
        assertNotNull(result);
        assertEquals("[{\"id\":1,\"name\":\"entity1\"},{\"id\":2,\"name\":\"entity2\"}]", new String(result));
    }

    @Test
    void shouldSerializeNullAsNullString() {
        JsonbSerializer<MyEntity> serializer = new JsonbSerializer<>();
        byte[] results = serializer.serialize("topic", null);
        assertNotNull(results);
        assertEquals("null", new String(results));
    }

    @Test
    void shouldSerializeNullAsNull() {
        JsonbSerializer<MyEntity> serializer = new JsonbSerializer<>();
        serializer.configure(Map.of(JsonbSerializer.NULL_AS_NULL_CONFIG, "true"), false);
        byte[] results = serializer.serialize("topic", null);
        assertNull(results);
    }

}
