package io.quarkus.sbom;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ResolvedDependency;

public class ApplicationComponent {

    public static final String SCOPE_RUNTIME = "runtime";
    public static final String SCOPE_DEVELOPMENT = "development";
    public static final String SCOPE_EXCLUDED = "excluded";

    public static final String TYPE_FRAMEWORK = "framework";

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ApplicationComponent {

        private Builder() {
            super();
        }

        public Builder(ApplicationComponent component) {
            super(component);
        }

        public Builder setVersion(String version) {
            this.version = version;
            return this;
        }

        public Builder setPath(Path componentPath) {
            path = componentPath;
            return this;
        }

        public Builder setDistributionPath(String distributionPath) {
            this.distributionPath = distributionPath;
            return this;
        }

        public Builder setResolvedDependency(ResolvedDependency dep) {
            this.dep = dep;
            if (dependencies.isEmpty()) {
                dependencies = dep.getDependencies();
            }
            return this;
        }

        public Builder setPedigree(String pedigree) {
            this.pedigree = pedigree;
            return this;
        }

        public Builder setDevelopmentScope() {
            return setScope("development");
        }

        public Builder setScope(String scope) {
            this.scope = scope;
            return this;
        }

        public Builder setDependencies(Collection<ArtifactCoords> dependencies) {
            this.dependencies = dependencies;
            return this;
        }

        /**
         * Sets the Maven coordinates of a synthetic component that is not backed by a
         * {@link ResolvedDependency} (e.g. a platform-member product component). The
         * coordinates drive the component's PURL, bom-ref, group, name and version.
         */
        public Builder setCoordinates(ArtifactCoords coordinates) {
            this.coordinates = coordinates;
            return this;
        }

        /**
         * Overrides the component display name (otherwise derived from the coordinates or path).
         */
        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the CPE of the component (used for platform-member product components).
         */
        public Builder setCpe(String cpe) {
            this.cpe = cpe;
            return this;
        }

        /**
         * Sets the CycloneDX component type (e.g. {@link #TYPE_FRAMEWORK}). When unset, the
         * generator derives the type (library for Maven artifacts, file for generic files).
         */
        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the artifacts this component <em>provides</em> (CycloneDX 1.6 {@code dependency.provides}).
         * Used for platform-member product components that provide their attributed artifacts.
         */
        public Builder setProvides(Collection<ArtifactCoords> provides) {
            this.provides = provides;
            return this;
        }

        public ApplicationComponent build() {
            return ensureImmutable();
        }

        @Override
        protected ApplicationComponent ensureImmutable() {
            return new ApplicationComponent(this);
        }
    }

    protected String version;
    protected Path path;
    protected String distributionPath;
    protected ResolvedDependency dep;
    protected String pedigree;
    protected String scope;
    protected Collection<ArtifactCoords> dependencies = List.of();
    protected ArtifactCoords coordinates;
    protected String name;
    protected String description;
    protected String cpe;
    protected String type;
    protected Collection<ArtifactCoords> provides = List.of();

    private ApplicationComponent() {
    }

    private ApplicationComponent(ApplicationComponent builder) {
        this.version = builder.version;
        this.path = builder.path;
        this.distributionPath = builder.distributionPath;
        this.dep = builder.dep;
        this.pedigree = builder.pedigree;
        this.scope = builder.scope;
        this.dependencies = List.copyOf(builder.dependencies);
        this.coordinates = builder.coordinates;
        this.name = builder.name;
        this.description = builder.description;
        this.cpe = builder.cpe;
        this.type = builder.type;
        this.provides = List.copyOf(builder.provides);
    }

    public String getVersion() {
        return version == null ? (dep == null ? null : dep.getVersion()) : version;
    }

    public Path getPath() {
        return path;
    }

    public String getDistributionPath() {
        return distributionPath;
    }

    public ResolvedDependency getResolvedDependency() {
        return dep;
    }

    public String getPedigree() {
        return pedigree;
    }

    public String getScope() {
        return scope == null ? (dep == null || dep.isRuntimeCp() ? SCOPE_RUNTIME : SCOPE_DEVELOPMENT) : scope;
    }

    public Collection<ArtifactCoords> getDependencies() {
        return dependencies;
    }

    /**
     * Maven coordinates of this component: the explicitly set coordinates (for a synthetic component
     * such as a platform-member product), otherwise the coordinates of the backing
     * {@link ResolvedDependency}, or {@code null} if the component has neither (e.g. a generic file).
     */
    public ArtifactCoords getCoordinates() {
        if (coordinates != null) {
            return coordinates;
        }
        return dep == null ? null
                : ArtifactCoords.of(dep.getGroupId(), dep.getArtifactId(), dep.getClassifier(), dep.getType(),
                        dep.getVersion());
    }

    /**
     * Optional display name override, or {@code null} to derive it from the coordinates/path.
     */
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * The CPE of the component, or {@code null}.
     */
    public String getCpe() {
        return cpe;
    }

    /**
     * The CycloneDX component type (e.g. {@link #TYPE_FRAMEWORK}), or {@code null} to let the
     * generator derive it.
     */
    public String getType() {
        return type;
    }

    /**
     * The artifacts this component provides (CycloneDX 1.6 {@code dependency.provides}); never {@code null}.
     */
    public Collection<ArtifactCoords> getProvides() {
        return provides;
    }

    protected ApplicationComponent ensureImmutable() {
        return this;
    }
}
