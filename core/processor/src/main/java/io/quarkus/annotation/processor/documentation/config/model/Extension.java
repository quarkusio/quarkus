package io.quarkus.annotation.processor.documentation.config.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record Extension(String groupId, String artifactId, String name) {

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
}
