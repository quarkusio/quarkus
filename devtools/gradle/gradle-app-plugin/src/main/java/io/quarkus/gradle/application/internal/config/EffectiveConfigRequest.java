package io.quarkus.gradle.application.internal.config;

import java.io.File;
import java.util.Map;
import java.util.Set;

public record EffectiveConfigRequest(
        Map<String, String> platformProperties,
        String applicationName,
        String applicationVersion,
        Set<File> sourceDirectories,
        Map<String, String> commonBuildProperties,
        Map<String, String> outputBuildProperties,
        Map<String, String> operationForcedProperties,
        Map<String, ?> taskProperties,
        Map<String, ?> projectProperties,
        Map<String, String> environment,
        Map<String, String> systemProperties,
        Map<String, String> defaultProperties,
        String profile) {

    public EffectiveConfigRequest {
        platformProperties = Map.copyOf(platformProperties);
        sourceDirectories = Set.copyOf(sourceDirectories);
        commonBuildProperties = Map.copyOf(commonBuildProperties);
        outputBuildProperties = Map.copyOf(outputBuildProperties);
        operationForcedProperties = Map.copyOf(operationForcedProperties);
        taskProperties = Map.copyOf(taskProperties);
        projectProperties = Map.copyOf(projectProperties);
        environment = Map.copyOf(environment);
        systemProperties = Map.copyOf(systemProperties);
        defaultProperties = Map.copyOf(defaultProperties);
        profile = profile == null || profile.isBlank() ? "prod" : profile;
    }
}
