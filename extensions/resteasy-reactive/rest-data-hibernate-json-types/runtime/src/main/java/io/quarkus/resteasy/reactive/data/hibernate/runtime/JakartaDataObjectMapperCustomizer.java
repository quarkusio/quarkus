package io.quarkus.resteasy.reactive.data.hibernate.runtime;

import jakarta.inject.Singleton;

import io.quarkus.jackson.JsonMapperBuilderCustomizer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

@Singleton
public class JakartaDataObjectMapperCustomizer implements JsonMapperBuilderCustomizer {

    @Override
    public void customize(JsonMapper.Builder builder) {
        SimpleModule module = new SimpleModule("quarkus-rest-data-hibernate-json-types");
        module.addSerializer(new PageSerializer());
        builder.addModule(module);
    }
}
