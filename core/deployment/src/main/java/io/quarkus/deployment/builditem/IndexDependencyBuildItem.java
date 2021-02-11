package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item that defines dependencies that should be indexed. This can be used when a dependency does not contain
 * a marker file (e.g. META-INF/beans.xml).
 */
public final class IndexDependencyBuildItem extends MultiBuildItem {
    private final String groupId;
    private final String artifactId;
    private final String classifier;

    public IndexDependencyBuildItem(String groupId, String artifactId) {
        this(groupId, artifactId, null);
    }

    public IndexDependencyBuildItem(String groupId, String artifactId, String classifier) {
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

    public String getClassifier() {
        return classifier;
    }
}
