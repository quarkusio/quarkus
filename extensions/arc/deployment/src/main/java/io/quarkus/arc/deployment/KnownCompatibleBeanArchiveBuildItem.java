package io.quarkus.arc.deployment;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Marks a bean archive with given coordinates (groupId, artifactId and optionally classifier) as known compatible with
 * Quarkus. This is only useful for bean archives whose {@code beans.xml} defines a bean discovery mode of {@code all};
 * bean archives with discovery mode of {@code none} or {@code annotated} are always compatible. If a bean archive is
 * known to be compatible with Quarkus, no warning about {@code all} discovery is logged during application build.
 */
public final class KnownCompatibleBeanArchiveBuildItem extends MultiBuildItem {
    final String groupId;
    final String artifactId;
    final String classifier;

    public KnownCompatibleBeanArchiveBuildItem(String groupId, String artifactId) {
        this(groupId, artifactId, "");
    }

    public KnownCompatibleBeanArchiveBuildItem(String groupId, String artifactId, String classifier) {
        this.groupId = Objects.requireNonNull(groupId, "groupId must be set");
        this.artifactId = Objects.requireNonNull(artifactId, "artifactId must be set");
        this.classifier = Objects.requireNonNull(classifier, "classifier must be set");
    }
}
