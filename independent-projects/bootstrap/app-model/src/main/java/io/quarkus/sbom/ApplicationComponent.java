package io.quarkus.sbom;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ResolvedDependency;

public class ApplicationComponent {

    public static final String SCOPE_RUNTIME = "runtime";
    public static final String SCOPE_DEVELOPMENT = "development";

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

        public ApplicationComponent build() {
            return ensureImmutable();
        }

        @Override
        protected ApplicationComponent ensureImmutable() {
            return new ApplicationComponent(this);
        }
    }

    protected Path path;
    protected String distributionPath;
    protected ResolvedDependency dep;
    protected String pedigree;
    protected String scope;
    protected Collection<ArtifactCoords> dependencies = List.of();

    private ApplicationComponent() {
    }

    private ApplicationComponent(ApplicationComponent builder) {
        this.path = builder.path;
        this.distributionPath = builder.distributionPath;
        this.dep = builder.dep;
        this.pedigree = builder.pedigree;
        this.scope = builder.scope;
        this.dependencies = List.copyOf(builder.dependencies);
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

    protected ApplicationComponent ensureImmutable() {
        return this;
    }
}
