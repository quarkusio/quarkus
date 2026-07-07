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

/**
 * Gradle task-input snapshot identifying an external artifact and the module POM used to enrich it.
 * <p>
 * The object-valued accessors are internal to task execution. The scalar accessors are the declared Gradle inputs,
 * which keeps task fingerprinting independent of Gradle/Maven implementation-object serialization. Natural ordering
 * is by artifact key and then POM GAV so callers can produce deterministic input collections.
 */
@SuppressWarnings("ClassCanBeRecord") // Gradle doesn't like records in this case
public class ExternalModuleDeclaredDependencyInput
        implements Serializable, Comparable<ExternalModuleDeclaredDependencyInput> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ArtifactKey artifactKey;
    private final GAV pomGav;

    /**
     * Creates a normalized task-input value.
     *
     * @param artifactKey resolved artifact identity
     * @param pomGav module POM coordinates
     * @throws NullPointerException if either value is {@code null}
     */
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

    /** @return the artifact key enriched during task execution */
    @Internal
    public ArtifactKey getArtifactKey() {
        return artifactKey;
    }

    /** @return coordinates of the effective POM used for enrichment */
    @Internal
    public GAV getPomGav() {
        return pomGav;
    }

    /** @return artifact group used as a scalar Gradle input */
    @Input
    public String getArtifactGroupId() {
        return artifactKey.getGroupId();
    }

    /** @return artifact name used as a scalar Gradle input */
    @Input
    public String getArtifactId() {
        return artifactKey.getArtifactId();
    }

    /** @return normalized artifact classifier used as a scalar Gradle input */
    @Input
    public String getArtifactClassifier() {
        return StrictDependencyDataCollector.defaultIfNull(artifactKey.getClassifier(), "");
    }

    /** @return normalized artifact type used as a scalar Gradle input */
    @Input
    public String getArtifactType() {
        return StrictDependencyDataCollector.defaultIfNull(artifactKey.getType(), "");
    }

    /** @return POM group used as a scalar Gradle input */
    @Input
    public String getPomGroupId() {
        return pomGav.getGroupId();
    }

    /** @return POM artifact name used as a scalar Gradle input */
    @Input
    public String getPomArtifactId() {
        return pomGav.getArtifactId();
    }

    /** @return POM version used as a scalar Gradle input */
    @Input
    public String getPomVersion() {
        return pomGav.getVersion();
    }

    /**
     * Orders inputs by artifact key and then by the textual POM GAV.
     */
    @Override
    public int compareTo(ExternalModuleDeclaredDependencyInput other) {
        int result = artifactKey.compareTo(other.artifactKey);
        if (result != 0) {
            return result;
        }
        return pomGav.toString().compareTo(other.pomGav.toString());
    }
}
