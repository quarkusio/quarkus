package io.quarkus.it.keycloak;

import jakarta.inject.Singleton;

import io.quarkus.jackson.JsonMapperBuilderCustomizer;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

/**
 * This class is used to alter the global ObjectMapper quarkus uses.
 * We ensure that KeyCloak Admin Client continues to work despite this.
 */
@Singleton
public class SnakeCaseObjectMapperCustomizer implements JsonMapperBuilderCustomizer {

    @Override
    public void customize(JsonMapper.Builder builder) {
        builder.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }
}
