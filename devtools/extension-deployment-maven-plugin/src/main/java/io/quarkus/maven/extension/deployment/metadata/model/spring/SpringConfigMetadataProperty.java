package io.quarkus.maven.extension.deployment.metadata.model.spring;

public record SpringConfigMetadataProperty(String name, String type, String description,
        String sourceType,
        // sourceMethod is only defined for groups in Spring Config metadata but we want it, so adding it there
        String sourceMethod,
        String defaultValue,
        SpringConfigMetadataDeprecation deprecation, QuarkusConfigAdditionalMetadataProperty quarkus) {

}
