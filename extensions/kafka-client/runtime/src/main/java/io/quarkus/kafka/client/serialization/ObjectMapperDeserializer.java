package io.quarkus.kafka.client.serialization;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class ObjectMapperDeserializer<T> implements Deserializer<T> {

    private final JavaType type;
    private final ObjectMapper objectMapper;

    public ObjectMapperDeserializer(Class<T> type) {
        this(type, ObjectMapperProducer.get());
    }

    public ObjectMapperDeserializer(Class<T> type, ObjectMapper objectMapper) {
        this.type = TypeFactory.defaultInstance().constructType(type);
        this.objectMapper = objectMapper;
    }

    public ObjectMapperDeserializer(TypeReference<T> typeReference) {
        this(typeReference, ObjectMapperProducer.get());
    }

    public ObjectMapperDeserializer(TypeReference<T> typeReference, ObjectMapper objectMapper) {
        this.type = TypeFactory.defaultInstance().constructType(typeReference);
        this.objectMapper = objectMapper;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try (InputStream is = new ByteArrayInputStream(data)) {
            return objectMapper.readValue(is, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
    }
}
