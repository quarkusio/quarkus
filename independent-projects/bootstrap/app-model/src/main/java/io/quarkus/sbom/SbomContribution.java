package io.quarkus.sbom;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import io.quarkus.maven.dependency.ArtifactCoords;

/**
 * Groups component descriptors and their dependency relationships for SBOM contribution.
 * <p>
 * An {@code SbomContribution} contains:
 * <ul>
 * <li>A flat collection of {@link ComponentDescriptor} instances (one per software component)</li>
 * <li>A collection of {@link ComponentDependencies} that describe the dependency graph
 * between components, referencing them by bom-ref</li>
 * </ul>
 * <p>
 * The {@link #mainComponentBomRef()} and {@link #runnerPath()} properties are reserved for the
 * Quarkus core contribution. Extensions should use {@link #of(Collection, Collection)} or
 * {@link #ofComponents(Collection)} to create their contributions.
 * <p>
 * Instances are immutable and safe for use by multiple threads. The {@link Builder} is not
 * thread-safe and should not be shared between threads.
 */
public final class SbomContribution {

    /**
     * Creates a contribution with both components and dependencies.
     *
     * @param components the component descriptors
     * @param dependencies the dependency relationships
     * @return a new contribution
     */
    public static SbomContribution of(Collection<ComponentDescriptor> components,
            Collection<ComponentDependencies> dependencies) {
        return new SbomContribution(components, dependencies, null, null, null);
    }

    /**
     * Creates a contribution with components only (no dependency relationships).
     *
     * @param components the component descriptors
     * @return a new contribution
     */
    public static SbomContribution ofComponents(Collection<ComponentDescriptor> components) {
        return new SbomContribution(components, List.of(), null, null, null);
    }

    private final Collection<ComponentDescriptor> components;
    private final Collection<ComponentDependencies> dependencies;
    private final String mainComponentBomRef;
    private final Path runnerPath;
    private final ArtifactCoords appArtifact;

    private SbomContribution(
            Collection<ComponentDescriptor> components,
            Collection<ComponentDependencies> dependencies,
            String mainComponentBomRef,
            Path runnerPath,
            ArtifactCoords appArtifact) {
        this.components = components == null || components.isEmpty()
                ? List.of()
                : List.copyOf(components);
        this.dependencies = dependencies == null || dependencies.isEmpty()
                ? List.of()
                : List.copyOf(dependencies);
        this.mainComponentBomRef = mainComponentBomRef;
        this.runnerPath = runnerPath;
        this.appArtifact = appArtifact;
    }

    public Collection<ComponentDescriptor> components() {
        return components;
    }

    public Collection<ComponentDependencies> dependencies() {
        return dependencies;
    }

    public String mainComponentBomRef() {
        return mainComponentBomRef;
    }

    public Path runnerPath() {
        return runnerPath;
    }

    /**
     * Returns the Maven coordinates of the application artifact that drives
     * the Quarkus build, or {@code null} for extension contributions.
     *
     * <p>
     * Used by the SBOM generator to resolve the project's own license from
     * the artifact's POM. This is necessary when the main component uses a
     * generic PURL (e.g. {@code pkg:generic/quarkus-run.jar}) that has no
     * Maven coordinates for POM resolution.
     * </p>
     *
     * @return the application artifact coordinates, or {@code null}
     */
    public ArtifactCoords appArtifact() {
        return appArtifact;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private List<ComponentDescriptor> components;
        private List<ComponentDependencies> dependencies;
        private String mainComponentBomRef;
        private Path runnerPath;
        private ArtifactCoords appArtifact;

        private Builder() {
        }

        public Builder setComponents(Collection<ComponentDescriptor> components) {
            this.components = components == null ? null : new ArrayList<>(components);
            return this;
        }

        public Builder setDependencies(Collection<ComponentDependencies> dependencies) {
            this.dependencies = dependencies == null ? null : new ArrayList<>(dependencies);
            return this;
        }

        public Builder addComponent(ComponentDescriptor component) {
            Objects.requireNonNull(component, "component is null");
            if (components == null) {
                components = new ArrayList<>();
            }
            components.add(component);
            return this;
        }

        public Builder addDependency(ComponentDependencies dependency) {
            Objects.requireNonNull(dependency, "dependency is null");
            if (dependencies == null) {
                dependencies = new ArrayList<>();
            }
            dependencies.add(dependency);
            return this;
        }

        Builder setMainComponentBomRef(String mainComponentBomRef) {
            this.mainComponentBomRef = mainComponentBomRef;
            return this;
        }

        Builder setRunnerPath(Path runnerPath) {
            this.runnerPath = runnerPath;
            return this;
        }

        Builder setAppArtifact(ArtifactCoords appArtifact) {
            this.appArtifact = appArtifact;
            return this;
        }

        public SbomContribution build() {
            return new SbomContribution(components, dependencies, mainComponentBomRef, runnerPath, appArtifact);
        }
    }
}
