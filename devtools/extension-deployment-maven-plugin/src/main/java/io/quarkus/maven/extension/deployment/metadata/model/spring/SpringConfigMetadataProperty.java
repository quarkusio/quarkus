package io.quarkus.maven.extension.deployment.metadata.model.spring;

public record SpringConfigMetadataProperty(String name, String type, String description,
        String sourceType,
        // these two are not in the Spring format but were explicitly requested
        String sourceField, String sourceMethod, String defaultValue,
        SpringConfigMetadataDeprecation deprecation, QuarkusConfigAdditionalMetadataProperty quarkus) {

}
