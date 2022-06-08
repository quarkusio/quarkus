package com.example.reactivejackson;

import javax.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.json.JsonMapper;

import io.quarkus.jackson.JsonMapperCustomizer;

@Singleton
public class DisableNullValueSerializationCustomizer implements JsonMapperCustomizer {

    @Override
    public void customize(JsonMapper.Builder builder) {
        // To suppress serializing properties with null values
        builder.serializationInclusion(JsonInclude.Include.NON_NULL);
    }
}
