package io.quarkus.gradle.model.pom;

import java.io.Serial;
import java.io.Serializable;

import io.quarkus.maven.dependency.ArtifactCoords;

public class DeclaredDependency implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String groupId;
    private final String artifactId;
    private final String classifier;
    private final String type;
    private final String version;
    private final String scope;
    private final boolean optional;

    DeclaredDependency(org.apache.maven.model.Dependency dep) {
        this.groupId = dep.getGroupId();
        this.artifactId = dep.getArtifactId();
        this.classifier = StrictDependencyDataCollector.defaultIfNull(dep.getClassifier(), ArtifactCoords.DEFAULT_CLASSIFIER);
        this.type = StrictDependencyDataCollector.defaultIfNull(dep.getType(), ArtifactCoords.TYPE_JAR);
        this.version = dep.getVersion();
        this.scope = StrictDependencyDataCollector.defaultIfNull(dep.getScope(),
                io.quarkus.maven.dependency.Dependency.SCOPE_COMPILE);
        this.optional = Boolean.parseBoolean(dep.getOptional());
    }

    String getGroupId() {
        return groupId;
    }

    String getArtifactId() {
        return artifactId;
    }

    String getClassifier() {
        return classifier;
    }

    String getType() {
        return type;
    }

    String getVersion() {
        return version;
    }

    String getScope() {
        return scope;
    }

    boolean isOptional() {
        return optional;
    }
}
