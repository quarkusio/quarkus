package io.quarkus.annotation.processor.documentation.config.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record Extension(String groupId, String artifactId, String name,
        NameSource nameSource, boolean detected) implements Comparable<Extension> {

    public static Extension createNotDetected() {
        return new Extension("not.detected", "not.detected", "Not detected", NameSource.NONE, false);
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
    public boolean splitOnConfigRootDescription() {
        // quarkus-core has a lot of config roots and they are very specific
        // we need to split them properly in the generated documentation
        return "io.quarkus".equals(groupId) && "quarkus-core".equals(artifactId);
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
