package io.quarkus.bootstrap.resolver.gradle.tooling;

import java.nio.file.Path;
import java.nio.file.Paths;

import io.quarkus.bootstrap.model.AppArtifact;

public class DefaultRemoteAppArtifact implements RemoteAppArtifact {

    private String groupId;
    private String artifactId;
    private String classifier;
    private String type;
    private String version;
    private String path;

    public DefaultRemoteAppArtifact() {

    }

    DefaultRemoteAppArtifact(String groupId, String artifactId, String classifier, String type, String version,
            String path) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier;
        this.type = type;
        this.version = version;
        this.path = path;
    }

    public static RemoteAppArtifact from(AppArtifact appArtifact) {
        return new DefaultRemoteAppArtifact(appArtifact.getGroupId(), appArtifact.getArtifactId(), appArtifact.getClassifier(),
                appArtifact.getType(), appArtifact.getVersion(),
                appArtifact.isResolved() ? appArtifact.getPath().toString() : null);
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
    public String getPath() {
        return path;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getVersion() {
        return version;
    }
}
