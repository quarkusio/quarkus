package io.quarkus.gradle.application.internal.codegen;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.gradle.application.internal.config.EffectiveConfigPlan;
import io.quarkus.runtime.LaunchMode;

public record CodegenRequest(
        Path appModel,
        LaunchMode launchMode,
        Set<File> sourceParentDirectories,
        Path generatedSourcesDirectory,
        Path buildDirectory,
        String projectDisplayName,
        List<String> codegenProviders,
        List<String> codegenInputNames,
        List<Path> classpath,
        EffectiveConfigPlan effectiveConfig,
        Map<String, String> buildSystemProperties) {

    public CodegenRequest {
        if (appModel == null) {
            throw new IllegalArgumentException("Quarkus application codegen request requires an application model");
        }
        if (launchMode == null) {
            throw new IllegalArgumentException("Quarkus application codegen request requires a launch mode");
        }
        sourceParentDirectories = Set.copyOf(sourceParentDirectories);
        if (generatedSourcesDirectory == null) {
            throw new IllegalArgumentException("Quarkus application codegen request requires a generated sources directory");
        }
        if (buildDirectory == null) {
            throw new IllegalArgumentException("Quarkus application codegen request requires a build directory");
        }
        if (projectDisplayName == null || projectDisplayName.isBlank()) {
            throw new IllegalArgumentException("Quarkus application codegen request requires a project display name");
        }
        codegenProviders = List.copyOf(codegenProviders);
        codegenInputNames = List.copyOf(codegenInputNames);
        classpath = List.copyOf(classpath);
        if (effectiveConfig == null) {
            throw new IllegalArgumentException("Quarkus application codegen request requires effective config");
        }
        buildSystemProperties = Map.copyOf(buildSystemProperties);
    }
}
