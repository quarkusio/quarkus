package io.quarkus.it.kafka.codecs;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

public class PetCodec implements Serializer<Pet>, Deserializer<Pet> {
    @Override
    public Pet deserialize(String topic, byte[] bytes) {
        String value = new String(bytes, StandardCharsets.UTF_8);
        String[] segments = value.split("_");
        Pet pet = new Pet();
        pet.setKind(segments[0]);
        pet.setName(segments[1]);
        return pet;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // no config
    }

    @Override
    public byte[] serialize(String topic, Pet pet) {
        return (pet.getKind() + "_" + pet.getName()).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void close() {
        // do nothing.
    }
}
