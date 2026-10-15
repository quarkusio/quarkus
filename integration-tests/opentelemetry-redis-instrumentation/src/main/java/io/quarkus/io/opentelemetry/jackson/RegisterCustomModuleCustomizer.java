package io.quarkus.io.opentelemetry.jackson;

import jakarta.inject.Singleton;

import io.quarkus.jackson.JsonMapperBuilderCustomizer;
import io.vertx.redis.client.impl.types.ErrorType;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

@Singleton
public class RegisterCustomModuleCustomizer implements JsonMapperBuilderCustomizer {

    public void customize(JsonMapper.Builder builder) {
        SimpleModule module = new SimpleModule();
        module.addSerializer(ErrorType.class, new ErrorTypeSerializer());
        builder.addModule(module);
    }
}
