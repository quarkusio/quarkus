package io.quarkus.annotation.processor.documentation.config.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record Extension(String groupId, String artifactId, String name,
        NameSource nameSource, boolean commonOrInternal, String guideUrl,
        boolean splitOnConfigRootDescription, boolean detected) implements Comparable<Extension> {

    private static final String ARTIFACT_COMMON_SUFFIX = "-common";
    private static final String ARTIFACT_INTERNAL_SUFFIX = "-internal";

    public static Extension of(String groupId, String artifactId, String name,
            NameSource nameSource, String guideUrl, boolean splitOnConfigRootDescription) {
        boolean commonOrInternal = artifactId.endsWith(ARTIFACT_COMMON_SUFFIX) || artifactId.endsWith(ARTIFACT_INTERNAL_SUFFIX);
        if (commonOrInternal) {
            nameSource = nameSource == NameSource.EXTENSION_METADATA ? NameSource.EXTENSION_METADATA_COMMON_INTERNAL
                    : (nameSource == NameSource.POM_XML ? NameSource.POM_XML_COMMON_INTERNAL : nameSource);
        }

        return new Extension(groupId, artifactId, name, nameSource, commonOrInternal, guideUrl, splitOnConfigRootDescription, true);
    }

    public static Extension createNotDetected() {
        return new Extension("not.detected", "not.detected", "Not detected", NameSource.NONE, false, null, false, false);
    }

    @Override
    public final String toString() {
        return groupId + ":" + artifactId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifactId, groupId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Extension other = (Extension) obj;
        return Objects.equals(artifactId, other.artifactId) && Objects.equals(groupId, other.groupId);
    }

    // TODO #42114 remove once fixed
    @Deprecated(forRemoval = true)
    @JsonIgnore
    public boolean isMixedModule() {
        return "io.quarkus".equals(groupId) && ("quarkus-core".equals(artifactId) || "quarkus-messaging".equals(artifactId));
    }

    @JsonIgnore
    public Extension normalizeCommonOrInternal() {
        if (!commonOrInternal()) {
            return this;
        }

        String normalizedArtifactId = artifactId;
        if (artifactId.endsWith(ARTIFACT_COMMON_SUFFIX)) {
            normalizedArtifactId = artifactId.substring(0, artifactId.length() - ARTIFACT_COMMON_SUFFIX.length());
        }
        if (artifactId.endsWith(ARTIFACT_INTERNAL_SUFFIX)) {
            normalizedArtifactId = artifactId.substring(0, artifactId.length() - ARTIFACT_INTERNAL_SUFFIX.length());
        }

        if (normalizedArtifactId.equals(artifactId)) {
            return this;
        }

        return new Extension(groupId, normalizedArtifactId, name, nameSource, commonOrInternal, null,
                splitOnConfigRootDescription, detected);
    }

    @Override
    public int compareTo(Extension other) {
        if (name != null && other.name != null) {
            int nameComparison = name.compareToIgnoreCase(other.name);
            if (nameComparison != 0) {
                return nameComparison;
            }
        }

        int groupIdComparison = groupId.compareToIgnoreCase(other.groupId);
        if (groupIdComparison != 0) {
            return groupIdComparison;
        }

        return artifactId.compareToIgnoreCase(other.artifactId);
    }

    public static enum NameSource {

        EXTENSION_METADATA(100),
        EXTENSION_METADATA_COMMON_INTERNAL(90),
        POM_XML(50),
        POM_XML_COMMON_INTERNAL(40),
        NONE(-1);

        private final int priority;

        NameSource(int priority) {
            this.priority = priority;
        }

        public boolean isBetterThan(NameSource other) {
            return this.priority > other.priority;
        }
    }
}
