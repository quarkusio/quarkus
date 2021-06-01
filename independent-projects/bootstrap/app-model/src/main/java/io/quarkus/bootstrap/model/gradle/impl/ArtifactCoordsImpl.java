package io.quarkus.bootstrap.model.gradle.impl;

import io.quarkus.bootstrap.model.gradle.ArtifactCoords;
import java.io.Serializable;
import java.util.Objects;

public class ArtifactCoordsImpl implements ArtifactCoords, Serializable {

    public static final String TYPE_JAR = "jar";

    private final String groupId;
    private final String artifactId;
    private final String classifier;
    private final String version;
    private final String type;

    public ArtifactCoordsImpl(String groupId, String artifactId, String version) {
        this(groupId, artifactId, "", version, TYPE_JAR);
    }

    public ArtifactCoordsImpl(String groupId, String artifactId, String classifier, String version, String type) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier;
        this.version = version;
        this.type = type;
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public String getClassifier() {
        return classifier;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ArtifactCoordsImpl that = (ArtifactCoordsImpl) o;
        return Objects.equals(groupId, that.groupId) &&
                Objects.equals(artifactId, that.artifactId) &&
                Objects.equals(classifier, that.classifier) &&
                Objects.equals(version, that.version) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, classifier, version, type);
    }
}
