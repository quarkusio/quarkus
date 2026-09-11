package io.quarkus.gradle.application.internal.execution;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.gradle.application.internal.config.EffectiveConfigPlan;
import io.quarkus.gradle.application.internal.planning.OutputLayout;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildDescriptor;

public record BuildRequest(
        QuarkusApplicationBuildDescriptor descriptor,
        Path outputRoot,
        Path appModel,
        List<Path> classpath,
        Set<File> sourceDirectories,
        EffectiveConfigPlan effectiveConfig,
        Map<String, String> buildSystemProperties,
        Map<String, String> operationForcedProperties,
        boolean processIsolated,
        OutputLayout outputLayout) {

    public BuildRequest {
        if (descriptor == null) {
            throw new IllegalArgumentException("Quarkus application build request requires a descriptor");
        }
        if (outputRoot == null) {
            throw new IllegalArgumentException("Quarkus application build request requires an output root");
        }
        if (appModel == null) {
            throw new IllegalArgumentException("Quarkus application build request requires an application model");
        }
        classpath = List.copyOf(classpath);
        sourceDirectories = Set.copyOf(sourceDirectories);
        if (effectiveConfig == null) {
            throw new IllegalArgumentException("Quarkus application build request requires effective config");
        }
        buildSystemProperties = Map.copyOf(buildSystemProperties);
        operationForcedProperties = Map.copyOf(operationForcedProperties);
        if (outputLayout == null) {
            throw new IllegalArgumentException("Quarkus application build request requires output layout");
        }
    }
}
