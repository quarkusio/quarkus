package io.quarkus.it.keycloak;

import jakarta.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.quarkus.jackson.ObjectMapperCustomizer;

/**
 * This class is used to alter the global ObjectMapper quarkus uses.
 * We ensure that KeyCloak Admin Client continues to work despite this.
 */
@Singleton
public class SnakeCaseObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper mapper) {
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }
}
