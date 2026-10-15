package io.quarkus.it.jackson;

import jakarta.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.jackson.JsonMapperBuilderCustomizer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Singleton
public class MyObjectMapperCustomizer implements JsonMapperBuilderCustomizer {

    @Override
    public void customize(JsonMapper.Builder builder) {
        builder.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
                .changeDefaultPropertyInclusion(
                        incl -> {
                            return incl.withValueInclusion(JsonInclude.Include.NON_NULL)
                                    .withValueInclusion(JsonInclude.Include.NON_ABSENT);
                        });
    }
}
