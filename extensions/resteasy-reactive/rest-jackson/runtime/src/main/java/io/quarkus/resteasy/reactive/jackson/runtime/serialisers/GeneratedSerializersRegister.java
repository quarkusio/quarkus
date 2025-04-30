package io.quarkus.resteasy.reactive.jackson.runtime.serialisers;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Singleton;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.quarkus.jackson.ObjectMapperCustomizer;

@Singleton
public class GeneratedSerializersRegister implements ObjectMapperCustomizer {

    private static final SimpleModule mappingModule = new SimpleModule();
    private static final ExactSerializers serializers = new ExactSerializers();

    static {
        // Use a custom SimpleSerializers to use a json serializer only if it has been generated for that
        // exact class and not one of its sublclasses. This is already the default behaviour for deserializers.
        mappingModule.setSerializers(serializers);
    }

    @Override
    public void customize(ObjectMapper objectMapper) {
        objectMapper.registerModule(mappingModule);
    }

    public static void addSerializer(Class<? extends StdSerializer> serClass) {
        try {
            StdSerializer serializer = serClass.getConstructor().newInstance();
            serializers.addExactSerializer(serializer.handledType(), serializer);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addDeserializer(Class<? extends StdDeserializer> deserClass) {
        try {
            StdDeserializer deserializer = deserClass.getConstructor().newInstance();
            mappingModule.addDeserializer(deserializer.handledType(), deserializer);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static class ExactSerializers extends SimpleSerializers {

        private final Map<Class<?>, JsonSerializer<?>> exactSerializers = new HashMap<>();

        public <T> void addExactSerializer(Class<? extends T> type, JsonSerializer<T> ser) {
            exactSerializers.put(type, ser);
        }

        @Override
        public JsonSerializer<?> findSerializer(SerializationConfig config, JavaType type, BeanDescription beanDesc) {
            JsonSerializer<?> exactSerializer = exactSerializers.get(type.getRawClass());
            return exactSerializer != null ? exactSerializer : super.findSerializer(config, type, beanDesc);
        }
    }
}
