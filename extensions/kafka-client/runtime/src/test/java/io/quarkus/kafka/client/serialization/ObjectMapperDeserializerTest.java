package io.quarkus.kafka.client.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

class ObjectMapperDeserializerTest {
    @Test
    void shouldDeserializeEntity() {
        MyEntity expected = new MyEntity(1, "entity1");
        ObjectMapperDeserializer<MyEntity> deserializer = new ObjectMapperDeserializer<>(MyEntity.class);
        MyEntity actual = deserializer.deserialize("topic", "{\"id\":1,\"name\":\"entity1\"}".getBytes());
        assertNotNull(actual);
        assertEquals(expected, actual);
    }

    @Test
    void shouldDeserializeListOfEntities() {
        TypeReference<List<MyEntity>> listType = new TypeReference<>() {
        };
        ObjectMapperDeserializer<List<MyEntity>> deserializer = new ObjectMapperDeserializer<>(listType);
        List<MyEntity> actuals = deserializer.deserialize("topic",
                "[{\"id\":1,\"name\":\"entity1\"},{\"id\":2,\"name\":\"entity2\"}]".getBytes());
        assertNotNull(actuals);
        assertEquals(2, actuals.size());
    }

    @Test
    void shouldDeserializeNullAsNullString() {
        ObjectMapperDeserializer<MyEntity> deserializer = new ObjectMapperDeserializer<>(MyEntity.class);
        MyEntity results = deserializer.deserialize("topic", "null".getBytes());
        assertNull(results);
    }

    @Test
    void shouldDeserializeNullAsNull() {
        ObjectMapperDeserializer<MyEntity> deserializer = new ObjectMapperDeserializer<>(MyEntity.class);
        MyEntity results = deserializer.deserialize("topic", null);
        assertNull(results);
    }
}
