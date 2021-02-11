package io.quarkus.bootstrap.resolver.model;

public interface ArtifactCoords {

    String getGroupId();

    String getArtifactId();

    String getClassifier();

    String getVersion();

    String getType();
}
