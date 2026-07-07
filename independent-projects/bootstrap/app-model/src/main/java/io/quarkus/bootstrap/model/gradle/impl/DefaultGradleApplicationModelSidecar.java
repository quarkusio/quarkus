package io.quarkus.bootstrap.model.gradle.impl;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecar;
import io.quarkus.bootstrap.model.gradle.GradleModelCorrelation;
import io.quarkus.bootstrap.model.gradle.GradleProjectComponent;
import io.quarkus.bootstrap.model.gradle.GradleProjectIdentity;

/**
 * Default serializable value used by Gradle producers to transport a {@link GradleApplicationModelSidecar}.
 * <p>
 * This is a concrete shared producer/Tooling API representation, not a Gradle user DSL type.
 */
public final class DefaultGradleApplicationModelSidecar implements GradleApplicationModelSidecar {

    private static final long serialVersionUID = 6540011924193033765L;

    private final GradleModelCorrelation correlation;
    private final GradleProjectIdentity targetProject;
    private final List<? extends GradleProjectComponent> projectComponents;

    /**
     * Creates a sidecar value.
     *
     * @param correlation correlation data for the paired application model; must not be {@code null}
     * @param targetProject identity of the Tooling API target project; must not be {@code null}
     * @param projectComponents components observed by the producer; must not be {@code null} or contain {@code null}
     *        elements
     * @throws NullPointerException if a required argument or component is {@code null}
     */
    public DefaultGradleApplicationModelSidecar(GradleModelCorrelation correlation,
            GradleProjectIdentity targetProject, Collection<? extends GradleProjectComponent> projectComponents) {
        this.correlation = Objects.requireNonNull(correlation, "correlation");
        this.targetProject = Objects.requireNonNull(targetProject, "targetProject");
        this.projectComponents = List.copyOf(projectComponents);
    }

    @Override
    public GradleModelCorrelation getCorrelation() {
        return correlation;
    }

    @Override
    public GradleProjectIdentity getTargetProject() {
        return targetProject;
    }

    @Override
    public List<? extends GradleProjectComponent> getProjectComponents() {
        return projectComponents;
    }
}
