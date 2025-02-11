package io.quarkus.resteasy.reactive.jackson.runtime.serialisers;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Singleton;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.quarkus.jackson.ObjectMapperCustomizer;
import io.quarkus.resteasy.reactive.jackson.runtime.ResteasyReactiveServerJacksonRecorder;

@Singleton
public class GeneratedSerializersRegister implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        objectMapper.registerModule(MappingModuleHolder.mappingModule);
    }

    static class MappingModuleHolder {
        static final Module mappingModule = createMappingModule();

        private static Module createMappingModule() {
            SimpleModule module = new SimpleModule();
            // Use a custom SimpleSerializers to use a json serializer only if it has been generated for that
            // exact class and not one of its sublclasses. This is already the default behaviour for deserializers.
            ExactSerializers serializers = new ExactSerializers();

            for (Class<? extends StdSerializer> serClass : ResteasyReactiveServerJacksonRecorder.getGeneratedSerializers()) {
                try {
                    StdSerializer serializer = serClass.getConstructor().newInstance();
                    serializers.addExactSerializer(serializer.handledType(), serializer);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                        | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }

            module.setSerializers(serializers);

            for (Class<? extends StdDeserializer> deserClass : ResteasyReactiveServerJacksonRecorder
                    .getGeneratedDeserializers()) {
                try {
                    StdDeserializer deserializer = deserClass.getConstructor().newInstance();
                    module.addDeserializer(deserializer.handledType(), deserializer);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                        | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }

            return module;
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
