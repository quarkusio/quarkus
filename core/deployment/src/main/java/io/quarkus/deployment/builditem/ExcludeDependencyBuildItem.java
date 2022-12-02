package io.quarkus.deployment.builditem;

import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item that defines dependencies that should not be indexed. This can be used when a dependency contains
 * a marker file (e.g. META-INF/beans.xml).
 */
public final class ExcludeDependencyBuildItem extends MultiBuildItem {
    private final String groupId;
    private final String artifactId;
    private final Optional<String> classifier;

    public ExcludeDependencyBuildItem(String groupId, String artifactId) {
        this(groupId, artifactId, Optional.empty());
    }

    public ExcludeDependencyBuildItem(String groupId, String artifactId, Optional<String> classifier) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public Optional<String> getClassifier() {
        return classifier;
    }
}
