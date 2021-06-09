package io.quarkus.bootstrap.model.gradle;

public interface ArtifactCoords {

    String getGroupId();

    String getArtifactId();

    String getClassifier();

    String getVersion();

    String getType();
}
