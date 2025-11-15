package io.quarkus.hibernate.orm.formatmapper;

import jakarta.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.jackson.ObjectMapperCustomizer;

@Singleton
public class MyObjectMapperCustomizer implements ObjectMapperCustomizer {
    @Override
    public void customize(ObjectMapper objectMapper) {
        // we don't really have to do anything here, it is enough that we have the customizer...
    }
}
