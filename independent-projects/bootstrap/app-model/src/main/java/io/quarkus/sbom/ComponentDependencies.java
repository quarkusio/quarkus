package io.quarkus.sbom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Represents the dependency relationships of a single component in an SBOM.
 * <p>
 * Each instance captures which other components (identified by their bom-ref strings)
 * a given component depends on, along with an optional dependency type for SPDX compatibility.
 * <p>
 * In CycloneDX, the {@code dependencyType} is not used (all relationships are plain dependencies).
 * In SPDX, the type distinguishes between {@code DEPENDS_ON}, {@code DEV_DEPENDENCY_OF}, etc.
 */
public final class ComponentDependencies {

    /**
     * Creates a {@code ComponentDependencies} with no dependency type.
     *
     * @param bomRef the bom-ref of the component that has these dependencies
     * @param dependsOn the bom-refs of the components it depends on
     * @return a new instance
     */
    public static ComponentDependencies of(String bomRef, Collection<String> dependsOn) {
        return new ComponentDependencies(bomRef, null, dependsOn);
    }

    public static Builder builder() {
        return new Builder();
    }

    private final String bomRef;
    private final String dependencyType;
    private final Collection<String> dependsOn;

    private ComponentDependencies(String bomRef, String dependencyType, Collection<String> dependsOn) {
        this.bomRef = Objects.requireNonNull(bomRef, "bomRef is required");
        this.dependencyType = dependencyType;
        this.dependsOn = dependsOn == null || dependsOn.isEmpty()
                ? List.of()
                : List.copyOf(dependsOn);
    }

    /**
     * The bom-ref of the component whose dependencies are described.
     */
    public String getBomRef() {
        return bomRef;
    }

    /**
     * The relationship type (e.g., "DEPENDS_ON", "DEV_DEPENDENCY_OF").
     * May be null when the SBOM format does not support typed dependencies.
     */
    public String getDependencyType() {
        return dependencyType;
    }

    /**
     * The bom-refs of the components this component depends on.
     */
    public Collection<String> getDependsOn() {
        return dependsOn;
    }

    public static class Builder {

        private String bomRef;
        private String dependencyType;
        private List<String> dependsOn;

        private Builder() {
        }

        public Builder setBomRef(String bomRef) {
            this.bomRef = bomRef;
            return this;
        }

        public Builder setDependencyType(String dependencyType) {
            this.dependencyType = dependencyType;
            return this;
        }

        public Builder setDependsOn(Collection<String> dependsOn) {
            this.dependsOn = dependsOn == null ? null : new ArrayList<>(dependsOn);
            return this;
        }

        public Builder addDependsOn(String bomRef) {
            if (dependsOn == null) {
                dependsOn = new ArrayList<>();
            }
            dependsOn.add(bomRef);
            return this;
        }

        public ComponentDependencies build() {
            return new ComponentDependencies(bomRef, dependencyType, dependsOn);
        }
    }
}
