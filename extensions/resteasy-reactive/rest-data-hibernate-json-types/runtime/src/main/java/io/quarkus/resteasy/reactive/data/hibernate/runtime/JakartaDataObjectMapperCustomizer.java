package io.quarkus.resteasy.reactive.data.hibernate.runtime;

import jakarta.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.quarkus.jackson.ObjectMapperCustomizer;

@Singleton
public class JakartaDataObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        SimpleModule module = new SimpleModule("quarkus-rest-data-hibernate-json-types");
        module.addSerializer(new PageSerializer());
        objectMapper.registerModule(module);
    }
}
