package com.example.reactivejackson;

import jakarta.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.jackson.JsonMapperBuilderCustomizer;
import tools.jackson.databind.json.JsonMapper;

@Singleton
public class DisableNullValueSerializationCustomizer implements JsonMapperBuilderCustomizer {

    @Override
    public void customize(JsonMapper.Builder builder) {
        builder.changeDefaultPropertyInclusion(
                incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL));
    }
}
