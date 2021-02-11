package io.quarkus.kafka.client.serialization;

import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A {@link Serde} that (de-)serializes JSON using Jackson's ObjectMapper.
 */
public class ObjectMapperSerde<T> implements Serde<T> {

    private final ObjectMapperSerializer<T> serializer;
    private final ObjectMapperDeserializer<T> deserializer;

    public ObjectMapperSerde(Class<T> type) {
        this(type, ObjectMapperProducer.get());
    }

    public ObjectMapperSerde(Class<T> type, ObjectMapper objectMapper) {
        this.serializer = new ObjectMapperSerializer<T>(objectMapper);
        this.deserializer = new ObjectMapperDeserializer<T>(type, objectMapper);
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public void close() {
        serializer.close();
        deserializer.close();
    }

    @Override
    public Serializer<T> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<T> deserializer() {
        return deserializer;
    }
}
