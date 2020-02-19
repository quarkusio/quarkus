package io.quarkus.optaplanner.jackson;

import javax.inject.Singleton;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.persistence.jackson.api.OptaPlannerJacksonModule;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.jackson.ObjectMapperCustomizer;

/**
 * OptaPlanner doesn't use Jackson, but it does have optional Jackson support for {@link Score}, etc.
 */
@Singleton
public class OptaPlannerObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        objectMapper.registerModule(OptaPlannerJacksonModule.createModule());
    }

}
