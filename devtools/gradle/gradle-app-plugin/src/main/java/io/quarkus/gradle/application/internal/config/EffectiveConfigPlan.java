package io.quarkus.gradle.application.internal.config;

import java.util.List;
import java.util.Map;

public record EffectiveConfigPlan(
        Map<String, String> fullValues,
        Map<String, String> quarkusWorkerValues,
        Map<String, String> buildSystemProperties,
        Map<String, String> descriptorShapeValues,
        List<EffectiveConfigDiagnostic> diagnostics,
        int externallyProvidedValuesOmitted,
        List<String> configSourceNames) {

    public EffectiveConfigPlan {
        fullValues = Map.copyOf(fullValues);
        quarkusWorkerValues = Map.copyOf(quarkusWorkerValues);
        buildSystemProperties = Map.copyOf(buildSystemProperties);
        descriptorShapeValues = Map.copyOf(descriptorShapeValues);
        diagnostics = List.copyOf(diagnostics);
        if (externallyProvidedValuesOmitted < 0) {
            throw new IllegalArgumentException("Omitted external value count must not be negative");
        }
        configSourceNames = List.copyOf(configSourceNames);
    }

    public EffectiveConfigPlan(Map<String, String> fullValues,
            Map<String, String> quarkusWorkerValues,
            Map<String, String> buildSystemProperties,
            Map<String, String> descriptorShapeValues) {
        this(fullValues, quarkusWorkerValues, buildSystemProperties, descriptorShapeValues, List.of(), 0, List.of());
    }
}
