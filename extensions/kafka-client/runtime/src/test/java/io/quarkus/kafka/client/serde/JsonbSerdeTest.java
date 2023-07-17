package io.quarkus.kafka.client.serde;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import org.junit.jupiter.api.Test;

import io.quarkus.kafka.client.serialization.JsonbSerde;

public class JsonbSerdeTest {

    @Test
    public void shouldSerializeAndDeserializeEntity() {
        MyEntity entity = new MyEntity();
        entity.id = 42L;
        entity.name = "Bob";

        try (JsonbSerde<MyEntity> serde = new JsonbSerde<>(MyEntity.class)) {
            byte[] serialized = serde.serializer().serialize("my-topic", entity);
            MyEntity deserialized = serde.deserializer().deserialize("my-topic", serialized);

            assertThat(deserialized.id).isEqualTo(42L);
            assertThat(deserialized.name).isEqualTo("Bob");
        }
    }

    @Test
    public void shouldSerializeAndDeserializeEntityWithGivenJsonb() throws Exception {
        MyEntity entity = new MyEntity();
        entity.id = 42L;
        entity.name = "Bob";

        try (Jsonb jsonb = JsonbBuilder.create(); JsonbSerde<MyEntity> serde = new JsonbSerde<>(MyEntity.class, jsonb)) {
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
