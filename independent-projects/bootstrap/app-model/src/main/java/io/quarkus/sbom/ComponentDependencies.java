package io.quarkus.sbom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Represents the dependency relationships of a single component in an SBOM.
 * <p>
 * Each instance captures which other components (identified by their bom-ref strings)
 * a given component relates to, split by relationship kind:
 * <ul>
 * <li>{@link #getDependsOn() dependsOn} &mdash; the components this component depends on;</li>
 * <li>{@link #getProvides() provides} &mdash; the components this component provides or implements
 * (CycloneDX 1.6 {@code dependency.provides}), used e.g. to attribute a platform member's artifacts
 * to the member's product component.</li>
 * </ul>
 * along with an optional dependency type for SPDX compatibility.
 * <p>
 * The two relationship collections are independent: a component may declare {@code dependsOn} refs,
 * {@code provides} refs, or (in principle) both. The current producers only ever populate one of them
 * per instance, but the API is shaped so that supporting a mix later is purely additive.
 * <p>
 * In CycloneDX, the {@code dependencyType} is not used (relationships are expressed as {@code dependsOn}
 * or {@code provides}). In SPDX, the type distinguishes between {@code DEPENDS_ON},
 * {@code DEV_DEPENDENCY_OF}, etc.
 */
public final class ComponentDependencies {

    /**
     * Creates a {@code ComponentDependencies} with only {@code dependsOn} relationships and no
     * dependency type.
     *
     * @param bomRef the bom-ref of the component that has these dependencies
     * @param dependsOn the bom-refs of the components it depends on
     * @return a new instance
     */
    public static ComponentDependencies of(String bomRef, Collection<String> dependsOn) {
        return new ComponentDependencies(bomRef, null, dependsOn, null);
    }

    /**
     * Creates a {@code ComponentDependencies} with only {@code provides} relationships and no
     * dependency type.
     *
     * @param bomRef the bom-ref of the component that provides these components
     * @param provides the bom-refs of the components it provides or implements
     * @return a new instance
     */
    public static ComponentDependencies provides(String bomRef, Collection<String> provides) {
        return new ComponentDependencies(bomRef, null, null, provides);
    }

    public static Builder builder() {
        return new Builder();
    }

    private final String bomRef;
    private final String dependencyType;
    private final Collection<String> dependsOn;
    private final Collection<String> provides;

    private ComponentDependencies(String bomRef, String dependencyType, Collection<String> dependsOn,
            Collection<String> provides) {
        this.bomRef = Objects.requireNonNull(bomRef, "bomRef is required");
        this.dependencyType = dependencyType;
        this.dependsOn = dependsOn == null || dependsOn.isEmpty()
                ? List.of()
                : List.copyOf(dependsOn);
        this.provides = provides == null || provides.isEmpty()
                ? List.of()
                : List.copyOf(provides);
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
     * Never {@code null}; empty when there are none.
     */
    public Collection<String> getDependsOn() {
        return dependsOn;
    }

    /**
     * The bom-refs of the components this component provides or implements
     * (CycloneDX 1.6 {@code dependency.provides}).
     * Never {@code null}; empty when there are none.
     */
    public Collection<String> getProvides() {
        return provides;
    }

    public static class Builder {

        private String bomRef;
        private String dependencyType;
        private List<String> dependsOn;
        private List<String> provides;

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

        public Builder setProvides(Collection<String> provides) {
            this.provides = provides == null ? null : new ArrayList<>(provides);
            return this;
        }

        public Builder addProvides(String bomRef) {
            if (provides == null) {
                provides = new ArrayList<>();
            }
            provides.add(bomRef);
            return this;
        }

        public ComponentDependencies build() {
            return new ComponentDependencies(bomRef, dependencyType, dependsOn, provides);
        }
    }
}
