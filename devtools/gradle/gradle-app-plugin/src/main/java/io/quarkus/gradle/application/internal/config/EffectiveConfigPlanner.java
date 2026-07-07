package io.quarkus.gradle.application.internal.config;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EffectiveConfigPlanner {

    public EffectiveConfigPlan plan(EffectiveConfigRequest request) {
        Map<String, String> buildProperties = new LinkedHashMap<>();
        buildProperties.putAll(request.commonBuildProperties());
        buildProperties.putAll(request.outputBuildProperties());

        Map<String, String> defaultProperties = new LinkedHashMap<>(request.defaultProperties());
        if (request.applicationName() != null && !request.applicationName().isBlank()) {
            defaultProperties.putIfAbsent("quarkus.application.name", request.applicationName());
        }
        if (request.applicationVersion() != null && !request.applicationVersion().isBlank()) {
            defaultProperties.putIfAbsent("quarkus.application.version", request.applicationVersion());
        }

        EffectiveConfig effectiveConfig = EffectiveConfig.builder()
                .withPlatformProperties(request.platformProperties())
                .withForcedProperties(request.operationForcedProperties())
                .withTaskProperties(request.taskProperties())
                .withBuildProperties(buildProperties)
                .withProjectProperties(request.projectProperties())
                .withDefaultProperties(defaultProperties)
                .withSystemProperties(request.systemProperties())
                .withEnvironmentProperties(request.environment())
                .withSourceDirectories(request.sourceDirectories())
                .withProfile(request.profile())
                .build();

        Map<String, String> buildSystemProperties = new LinkedHashMap<>(effectiveConfig.getQuarkusValues());
        buildSystemProperties.putAll(request.commonBuildProperties());
        buildSystemProperties.putAll(request.outputBuildProperties());
        buildSystemProperties.putAll(asStringMap(request.taskProperties()));
        buildSystemProperties.putAll(asStringMap(request.projectProperties()));
        buildSystemProperties.putAll(request.systemProperties());
        buildSystemProperties.putAll(request.operationForcedProperties());

        return new EffectiveConfigPlan(
                effectiveConfig.getValues(),
                effectiveConfig.getQuarkusValues(),
                buildSystemProperties,
                request.operationForcedProperties(),
                effectiveConfig.getDiagnostics(),
                effectiveConfig.externallyProvidedValuesOmitted(),
                effectiveConfig.getConfigSourceNames());
    }

    private Map<String, String> asStringMap(Map<String, ?> source) {
        Map<String, String> target = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (value != null) {
                target.put(key, value.toString());
            }
        });
        return target;
    }
}
