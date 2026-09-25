package io.quarkus.gradle.application.internal.modelgen;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.util.BootstrapUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;

final class ConditionalDependencyResolver {

    private ConditionalDependencyResolver() {
    }

    static List<String> conditionalDependencyCoordinates(Iterable<File> runtimeArtifacts) {
        Set<String> coordinates = new LinkedHashSet<>();
        for (File artifact : runtimeArtifacts) {
            ExtensionDescriptorReader.readDescriptor(artifact)
                    .map(properties -> conditionalDependencies(properties, BootstrapConstants.CONDITIONAL_DEPENDENCIES))
                    .ifPresent(coordinates::addAll);
        }
        return coordinates.stream().sorted().toList();
    }

    static List<String> conditionalDevDependencyCoordinates(Iterable<File> runtimeArtifacts) {
        Set<String> coordinates = new LinkedHashSet<>();
        for (File artifact : runtimeArtifacts) {
            ExtensionDescriptorReader.readDescriptor(artifact)
                    .map(properties -> conditionalDependencies(properties, BootstrapConstants.CONDITIONAL_DEV_DEPENDENCIES))
                    .ifPresent(coordinates::addAll);
        }
        return coordinates.stream().sorted().toList();
    }

    static List<String> satisfiedConditionalDependencyCoordinates(
            List<String> runtimeComponentKeys,
            List<String> conditionalArtifactRecords) {
        Set<ArtifactKey> existingKeys = new HashSet<>();
        for (String value : runtimeComponentKeys) {
            existingKeys.add(deserializeKey(value));
        }

        Map<ArtifactKey, ConditionalArtifact> candidates = new LinkedHashMap<>();
        for (String value : conditionalArtifactRecords) {
            ArtifactRecord artifact = ArtifactRecord.deserialize(value);
            ExtensionDescriptorReader.readDescriptor(artifact.file())
                    .ifPresent(properties -> candidates.put(artifact.key(), new ConditionalArtifact(artifact, properties)));
        }

        Set<String> satisfiedCoordinates = new LinkedHashSet<>();
        boolean changed;
        do {
            changed = false;
            var iterator = candidates.values().iterator();
            while (iterator.hasNext()) {
                ConditionalArtifact candidate = iterator.next();
                if (candidate.isSatisfiedBy(existingKeys)) {
                    satisfiedCoordinates.add(candidate.artifact().dependencyNotation());
                    existingKeys.add(candidate.artifact().key());
                    iterator.remove();
                    changed = true;
                }
            }
        } while (changed);

        return satisfiedCoordinates.stream().sorted().toList();
    }

    private static List<String> conditionalDependencies(Properties properties, String propertyName) {
        List<String> dependencies = new ArrayList<>();
        String rawDependencies = properties.getProperty(propertyName);
        if (rawDependencies == null || rawDependencies.isBlank()) {
            return dependencies;
        }

        String[] splitDependencies = BootstrapUtils.splitByWhitespace(rawDependencies);
        for (String rawDependency : splitDependencies) {
            ArtifactCoords coords = ArtifactCoords.fromString(rawDependency);
            dependencies.add(coords.getGroupId() + ":" + coords.getArtifactId() + ":" + coords.getVersion());
        }
        return dependencies;
    }

    static String artifactRecord(String coordinate, File file) {
        return ArtifactRecord.fromCoords(ArtifactCoords.fromString(coordinate), file).serialize();
    }

    static String serializeKey(ArtifactKey key) {
        return String.join("\t", key.getGroupId(), key.getArtifactId(), key.getClassifier(), key.getType());
    }

    private static ArtifactKey deserializeKey(String value) {
        String[] parts = value.split("\t", -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid artifact key record: " + value);
        }
        return ArtifactKey.of(parts[0], parts[1], parts[2], parts[3]);
    }

    private record ConditionalArtifact(ArtifactRecord artifact, Properties descriptor) {

        boolean isSatisfiedBy(Set<ArtifactKey> existingKeys) {
            if (!descriptor.containsKey(BootstrapConstants.DEPENDENCY_CONDITION)) {
                return true;
            }
            ArtifactKey[] conditions = BootstrapUtils
                    .parseDependencyCondition(descriptor.getProperty(BootstrapConstants.DEPENDENCY_CONDITION));
            for (ArtifactKey condition : conditions) {
                if (!containsMatchingKey(existingKeys, condition)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static boolean containsMatchingKey(Set<ArtifactKey> existingKeys, ArtifactKey condition) {
        if (existingKeys.contains(condition)) {
            return true;
        }
        if (condition.getType() != null) {
            return false;
        }
        for (ArtifactKey existingKey : existingKeys) {
            if (existingKey.getGroupId().equals(condition.getGroupId())
                    && existingKey.getArtifactId().equals(condition.getArtifactId())
                    && existingKey.getClassifier().equals(condition.getClassifier())) {
                return true;
            }
        }
        return false;
    }
}
