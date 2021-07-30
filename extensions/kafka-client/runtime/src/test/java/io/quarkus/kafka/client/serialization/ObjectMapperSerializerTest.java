package io.quarkus.kafka.client.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ObjectMapperSerializerTest {

    @Test
    void shouldSerializeEntity() {
        ObjectMapperSerializer<MyEntity> serializer = new ObjectMapperSerializer<>();
        MyEntity entity = new MyEntity(1, "entity1");
        byte[] result = serializer.serialize("topic", entity);
        assertNotNull(result);
        assertEquals("{\"id\":1,\"name\":\"entity1\"}", new String(result));
    }

    @Test
    void shouldSerializeNullAsNullString() {
        ObjectMapperSerializer<MyEntity> serializer = new ObjectMapperSerializer<>();
        byte[] results = serializer.serialize("topic", null);
        assertNotNull(results);
        assertEquals("null", new String(results));
    }

    @Test
    void shouldSerializeNullAsNull() {
        ObjectMapperSerializer<MyEntity> serializer = new ObjectMapperSerializer<>();
        serializer.configure(Map.of(ObjectMapperSerializer.NULL_AS_NULL_CONFIG, "true"), false);
        byte[] results = serializer.serialize("topic", null);
        assertNull(results);
    }

}
