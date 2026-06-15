package io.quarkus.deployment.builditem;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.maven.dependency.ArtifactCoords;

/**
 * Build item that defines dependencies that should be indexed. This can be used when a dependency does not contain
 * a marker file (e.g. META-INF/beans.xml).
 */
public final class IndexDependencyBuildItem extends MultiBuildItem {
    private final String groupId;
    private final String artifactId;
    private final String classifier;
    private final String type;

    public IndexDependencyBuildItem(String groupId, String artifactId) {
        this(groupId, artifactId, null, ArtifactCoords.TYPE_JAR);
    }

    public IndexDependencyBuildItem(String groupId, String artifactId, String classifier) {
        this(groupId, artifactId, classifier, ArtifactCoords.TYPE_JAR);
    }

    public IndexDependencyBuildItem(String groupId, String artifactId, String classifier, String type) {
        this.groupId = Objects.requireNonNull(groupId, "groupId must be set");
        this.artifactId = artifactId;
        this.classifier = classifier;
        this.type = Objects.requireNonNull(type, "type must be set");
    }

    /**
     *
     * @return the groupId, never {@code null}
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     *
     * @return the artifactId, or {@code null}
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     *
     * @return the classifier, or {@code null}
     */
    public String getClassifier() {
        return classifier;
    }

    /**
     *
     * @return the type, never {@code null}
     */
    public String getType() {
        return type;
    }
}
