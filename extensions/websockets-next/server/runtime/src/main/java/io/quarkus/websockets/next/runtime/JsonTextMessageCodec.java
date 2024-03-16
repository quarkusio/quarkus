package io.quarkus.websockets.next.runtime;

import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.websockets.next.TextMessageCodec;

@Singleton
@Priority(0)
public class JsonTextMessageCodec implements TextMessageCodec<Object> {

    private final ConcurrentMap<Type, JavaType> types = new ConcurrentHashMap<>();

    @Inject
    ObjectMapper mapper;

    @Override
    public String encode(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Object decode(Type type, String value) {
        try {
            return mapper.readValue(value, types.computeIfAbsent(type, this::computeJavaType));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private JavaType computeJavaType(Type type) {
        return mapper.getTypeFactory().constructType(type);
    }

    @Override
    public boolean supports(Type type) {
        return true;
    }

}
