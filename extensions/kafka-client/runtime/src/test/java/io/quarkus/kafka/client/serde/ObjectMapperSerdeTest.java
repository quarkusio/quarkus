package io.quarkus.kafka.client.serde;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.kafka.client.serialization.ObjectMapperSerde;

public class ObjectMapperSerdeTest {

    @Test
    public void shouldSerializeAndDeserializeEntity() {
        MyEntity entity = new MyEntity();
        entity.id = 42L;
        entity.name = "Bob";

        try (ObjectMapperSerde<MyEntity> serde = new ObjectMapperSerde<>(MyEntity.class)) {
            byte[] serialized = serde.serializer().serialize("my-topic", entity);
            MyEntity deserialized = serde.deserializer().deserialize("my-topic", serialized);

            assertThat(deserialized.id).isEqualTo(42L);
            assertThat(deserialized.name).isEqualTo("Bob");
        }
    }

    @Test
    public void shouldSerializeAndDeserializeEntityWithGivenObjectMapper() throws Exception {
        MyEntity entity = new MyEntity();
        entity.id = 42L;
        entity.name = "Bob";

        ObjectMapper objectMapper = new ObjectMapper();
        try (ObjectMapperSerde<MyEntity> serde = new ObjectMapperSerde<>(MyEntity.class, objectMapper)) {
            byte[] serialized = serde.serializer().serialize("my-topic", entity);
            MyEntity deserialized = serde.deserializer().deserialize("my-topic", serialized);

            assertThat(deserialized.id).isEqualTo(42L);
            assertThat(deserialized.name).isEqualTo("Bob");
        }
    }

    public static class MyEntity {
        public long id;
        public String name;
    }
}
