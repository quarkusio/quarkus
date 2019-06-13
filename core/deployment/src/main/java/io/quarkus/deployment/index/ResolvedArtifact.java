package io.quarkus.deployment.index;

import java.nio.file.Path;

public class ResolvedArtifact {

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String classifier;
    private final Path artifactPath;

    public ResolvedArtifact(String groupId, String artifactId, String version, String classifier, Path artifactPath) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
        this.artifactPath = artifactPath;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getClassifier() {
        return classifier;
    }

    public Path getArtifactPath() {
        return artifactPath;
    }

    @Override
    public String toString() {
        return "ResolvedArtifact{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                ", classifier='" + classifier + '\'' +
                '}';
    }
}
