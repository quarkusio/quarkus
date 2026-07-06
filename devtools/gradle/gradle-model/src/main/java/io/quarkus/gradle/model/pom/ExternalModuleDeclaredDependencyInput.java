package io.quarkus.gradle.model.pom;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GAV;

@SuppressWarnings("ClassCanBeRecord") // Gradle doesn't like records in this case
public class ExternalModuleDeclaredDependencyInput
        implements Serializable, Comparable<ExternalModuleDeclaredDependencyInput> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ArtifactKey artifactKey;
    private final GAV pomGav;

    public ExternalModuleDeclaredDependencyInput(ArtifactKey artifactKey, GAV pomGav) {
        this.artifactKey = Objects.requireNonNull(artifactKey, "artifactKey cannot be null");
        this.pomGav = Objects.requireNonNull(pomGav, "pomGav cannot be null");
    }

    static ExternalModuleDeclaredDependencyInput from(
            ResolvedArtifactResult artifact,
            ModuleComponentIdentifier moduleId) {
        return new ExternalModuleDeclaredDependencyInput(
                StrictDependencyDataCollector.resolveArtifactKey(artifact, moduleId),
                new GAV(moduleId.getGroup(), moduleId.getModule(), moduleId.getVersion()));
    }

    @Internal
    public ArtifactKey getArtifactKey() {
        return artifactKey;
    }

    @Internal
    public GAV getPomGav() {
        return pomGav;
    }

    @Input
    public String getArtifactGroupId() {
        return artifactKey.getGroupId();
    }

    @Input
    public String getArtifactId() {
        return artifactKey.getArtifactId();
    }

    @Input
    public String getArtifactClassifier() {
        return StrictDependencyDataCollector.defaultIfNull(artifactKey.getClassifier(), "");
    }

    @Input
    public String getArtifactType() {
        return StrictDependencyDataCollector.defaultIfNull(artifactKey.getType(), "");
    }

    @Input
    public String getPomGroupId() {
        return pomGav.getGroupId();
    }

    @Input
    public String getPomArtifactId() {
        return pomGav.getArtifactId();
    }

    @Input
    public String getPomVersion() {
        return pomGav.getVersion();
    }

    @Override
    public int compareTo(ExternalModuleDeclaredDependencyInput other) {
        int result = artifactKey.compareTo(other.artifactKey);
        if (result != 0) {
            return result;
        }
        return pomGav.toString().compareTo(other.pomGav.toString());
    }
}
