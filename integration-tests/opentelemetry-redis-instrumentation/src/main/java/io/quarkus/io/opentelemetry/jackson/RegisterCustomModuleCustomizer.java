package io.quarkus.io.opentelemetry.jackson;

import jakarta.inject.Singleton;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.module.SimpleModule;

import io.quarkus.jackson.ObjectMapperCustomizer;
import io.vertx.redis.client.impl.types.ErrorType;

@Singleton
public class RegisterCustomModuleCustomizer implements ObjectMapperCustomizer {

    public void customize(ObjectMapper mapper) {
        SimpleModule module = new SimpleModule();
        module.addSerializer(ErrorType.class, new ErrorTypeSerializer());
        mapper.registerModule(module);
    }
}
