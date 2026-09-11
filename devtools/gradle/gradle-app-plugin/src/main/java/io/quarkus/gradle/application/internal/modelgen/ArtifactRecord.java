package io.quarkus.gradle.application.internal.modelgen;

import java.io.File;
import java.nio.file.Path;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;

record ArtifactRecord(String groupId, String artifactId, String version, String classifier, String type, File file) {

    private static final String SEPARATOR = "\t";

    ArtifactKey key() {
        return ArtifactKey.of(groupId, artifactId, classifier, type);
    }

    String dependencyNotation() {
        return groupId + ":" + artifactId + ":" + version;
    }

    String serialize() {
        return String.join(SEPARATOR,
                groupId,
                artifactId,
                version,
                classifier,
                type,
                file.toPath().toAbsolutePath().normalize().toString());
    }

    static ArtifactRecord fromCoords(ArtifactCoords coords, File file) {
        return new ArtifactRecord(
                coords.getGroupId(),
                coords.getArtifactId(),
                coords.getVersion(),
                normalizeClassifier(coords.getClassifier()),
                coords.getType(),
                file);
    }

    static ArtifactRecord deserialize(String value) {
        String[] parts = value.split(SEPARATOR, -1);
        if (parts.length != 6) {
            throw new IllegalArgumentException("Invalid artifact record: " + value);
        }
        return new ArtifactRecord(parts[0], parts[1], parts[2], normalizeClassifier(parts[3]), parts[4],
                Path.of(parts[5]).toFile());
    }

    private static String normalizeClassifier(String classifier) {
        return classifier == null || classifier.isBlank() ? ArtifactCoords.DEFAULT_CLASSIFIER : classifier;
    }
}
