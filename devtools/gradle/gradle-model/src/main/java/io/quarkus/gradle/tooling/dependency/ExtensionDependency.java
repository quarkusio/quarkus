package io.quarkus.gradle.tooling.dependency;

import java.util.List;
import java.util.Objects;

import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import io.quarkus.maven.dependency.ArtifactKey;

public abstract class ExtensionDependency<T> {

    private final ModuleVersionIdentifier extensionId;
    private final T deploymentModule;
    private final List<Dependency> conditionalDependencies;
    private final List<ArtifactKey> dependencyConditions;
    private boolean isConditional;

    public ExtensionDependency(ModuleVersionIdentifier extensionId, T deploymentModule,
            List<Dependency> conditionalDependencies,
            List<ArtifactKey> dependencyConditions) {
        this.extensionId = extensionId;
        this.deploymentModule = deploymentModule;
        this.conditionalDependencies = conditionalDependencies;
        this.dependencyConditions = dependencyConditions;
    }

    public void importConditionalDependency(DependencyHandler dependencies, ModuleVersionIdentifier capability) {
        Dependency dependency = findConditionalDependency(capability);

        if (dependency == null) {
            throw new GradleException("Trying to add " + capability.getName() + " variant which is not declared by "
                    + extensionId.getName() + " extension.");
        }

        dependencies.components(handler -> handler.withModule(toModuleName(),
                componentMetadataDetails -> componentMetadataDetails.allVariants(variantMetadata -> variantMetadata
                        .withDependencies(d -> d.add(DependencyUtils.asDependencyNotation(dependency))))));
    }

    private Dependency findConditionalDependency(ModuleVersionIdentifier capability) {
        for (Dependency conditionalDependency : conditionalDependencies) {
            if (conditionalDependency.getGroup().equals(capability.getGroup())
                    && conditionalDependency.getName().equals(capability.getName())) {
                return conditionalDependency;
            }
        }
        return null;
    }

    public String toModuleName() {
        return String.join(":", this.extensionId.getGroup(), this.extensionId.getName());
    }

    public String getGroup() {
        return extensionId.getGroup();
    }

    public String getName() {
        return extensionId.getName();
    }

    public String getVersion() {
        return extensionId.getVersion();
    }

    public ModuleVersionIdentifier getExtensionId() {
        return extensionId;
    }

    public List<ArtifactKey> getDependencyConditions() {
        return dependencyConditions;
    }

    public List<Dependency> getConditionalDependencies() {
        return conditionalDependencies;
    }

    public T getDeploymentModule() {
        return deploymentModule;
    }

    public boolean isConditional() {
        return isConditional;
    }

    public void setConditional(boolean isConditional) {
        this.isConditional = isConditional;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ExtensionDependency<?> that = (ExtensionDependency<?>) o;
        return Objects.equals(extensionId, that.extensionId)
                && Objects.equals(conditionalDependencies, that.conditionalDependencies)
                && Objects.equals(dependencyConditions, that.dependencyConditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(extensionId, conditionalDependencies, dependencyConditions);
    }
}
