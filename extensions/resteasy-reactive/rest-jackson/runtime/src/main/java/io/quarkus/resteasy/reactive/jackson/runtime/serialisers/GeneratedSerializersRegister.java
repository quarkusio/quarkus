package io.quarkus.resteasy.reactive.jackson.runtime.serialisers;

import java.lang.reflect.InvocationTargetException;

import jakarta.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
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
        static final SimpleModule mappingModule = createMappingModule();

        private static SimpleModule createMappingModule() {
            SimpleModule module = new SimpleModule();

            for (Class<? extends StdSerializer> serClass : ResteasyReactiveServerJacksonRecorder.getGeneratedSerializers()) {
                try {
                    StdSerializer serializer = serClass.getConstructor().newInstance();
                    module.addSerializer(serializer.handledType(), serializer);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                        | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }

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
}
