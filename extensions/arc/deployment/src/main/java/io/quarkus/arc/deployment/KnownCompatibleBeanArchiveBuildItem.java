package io.quarkus.arc.deployment;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Marks a bean archive with given coordinates (groupId, artifactId and optionally classifier)
 * as known compatible with Quarkus. If a bean archive is known to be compatible with
 * Quarkus, any error logging or exception throwing related to that compatibility is skipped.
 * <p>
 * This is useful in the following cases:
 * <li>Bean archives whose {@code beans.xml} defines a bean discovery mode of {@code all}; bean archives with discovery mode of
 * {@code none} or {@code annotated} are always compatible.</li>
 * <li>Bean archives that contain the unsupported {@link jakarta.enterprise.inject.Specializes} annotation.</li>
 */
public final class KnownCompatibleBeanArchiveBuildItem extends MultiBuildItem {
    final Set<Reason> reasons;
    final String groupId;
    final String artifactId;
    final String classifier;

    /**
     * Deprecated, use {@link KnownCompatibleBeanArchiveBuildItem#builder(String, String)} method instead.
     * For compatibility reasons, this method automatically registers the artifact with {@link Reason#BEANS_XML_ALL}.
     */
    @Deprecated
    public KnownCompatibleBeanArchiveBuildItem(String groupId, String artifactId) {
        this(groupId, artifactId, "");
    }

    /**
     * Deprecated, use {@link KnownCompatibleBeanArchiveBuildItem#builder(String, String)} method instead.
     * For compatibility reasons, this method automatically registers the artifact with {@link Reason#BEANS_XML_ALL}.
     */
    @Deprecated
    public KnownCompatibleBeanArchiveBuildItem(String groupId, String artifactId, String classifier) {
        this(groupId, artifactId, classifier, Set.of(Reason.BEANS_XML_ALL));
    }

    private KnownCompatibleBeanArchiveBuildItem(String groupId, String artifactId, String classifier, Set<Reason> reasons) {
        Objects.requireNonNull(groupId, "groupId must be set");
        Objects.requireNonNull(artifactId, "artifactId must be set");
        Objects.requireNonNull(classifier, "classifier must be set");
        if (reasons.isEmpty()) {
            throw new IllegalStateException(
                    "KnownCompatibleBeanArchiveBuildItem.Builder needs to declare at least one compatibility reason. Artifact with following coordinates had no reason associated: "
                            + groupId + ":" + artifactId);
        }
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier;
        this.reasons = reasons;
    }

    public static Builder builder(String groupId, String artifactId) {
        return new Builder(groupId, artifactId);
    }

    /**
     * An enum listing known reasons for which an archive might be marked as compatible despite using some unsupported
     * feature such as {@code beans.xml} discovery mode {@code all} or using {@link jakarta.enterprise.inject.Specializes}
     * annotation on its classes.
     */
    public enum Reason {
        BEANS_XML_ALL,
        SPECIALIZES_ANNOTATION;
    }

    public static class Builder {

        private final String groupId;
        private final String artifactId;
        private final Set<Reason> reasons;
        private String classifier;

        private Builder(String groupId, String artifactId) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.classifier = "";
            this.reasons = new HashSet<>();
        }

        public Builder setClassifier(String classifier) {
            this.classifier = classifier;
            return this;
        }

        public Builder addReason(Reason reason) {
            this.reasons.add(reason);
            return this;
        }

        public KnownCompatibleBeanArchiveBuildItem build() {
            return new KnownCompatibleBeanArchiveBuildItem(groupId, artifactId, classifier, reasons);
        }
    }
}
