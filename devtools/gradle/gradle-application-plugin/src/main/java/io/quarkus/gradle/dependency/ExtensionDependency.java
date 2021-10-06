package io.quarkus.gradle.dependency;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;

public class ExtensionDependency {

    ModuleVersionIdentifier extensionId;
    AppArtifactCoords deploymentModule;
    List<Dependency> conditionalDependencies;
    List<ArtifactKey> dependencyConditions;

    ExtensionDependency(ModuleVersionIdentifier extensionId, AppArtifactCoords deploymentModule,
            List<Dependency> conditionalDependencies,
            List<ArtifactKey> dependencyConditions) {
        this.extensionId = extensionId;
        this.deploymentModule = deploymentModule;
        this.conditionalDependencies = conditionalDependencies;
        this.dependencyConditions = dependencyConditions;
    }

    public boolean needsResolution(Set<ResolvedArtifact> resolvedArtifacts) {
        for (Dependency dependency : conditionalDependencies) {
            if (!DependencyUtils.exists(resolvedArtifacts, dependency)) {
                return true;
            }
        }
        return false;
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

    public void createDeploymentVariant(DependencyHandler dependencies) {
        dependencies.components(handler -> handler.withModule(toModuleName(),
                componentMetadataDetails -> componentMetadataDetails
                        .addVariant(DependencyUtils.asDependencyNotation(deploymentModule),
                                variantMetadata -> {
                                    variantMetadata.withCapabilities(
                                            capabilities -> {
                                                capabilities.removeCapability(extensionId.getGroup(),
                                                        extensionId.getName());
                                                capabilities.addCapability(deploymentModule.getGroupId(),
                                                        deploymentModule.getArtifactId() + "-capability",
                                                        deploymentModule.getVersion());
                                            });
                                    variantMetadata.withDependencies(
                                            d -> {
                                                d.remove(this.asDependencyNotation());
                                                d.add(DependencyUtils.asDependencyNotation(deploymentModule));
                                            });
                                })));
    }

    public Dependency asDependency(DependencyHandler dependencies) {
        return dependencies.create(asDependencyNotation());
    }

    public String asDependencyNotation() {
        return String.join(":", this.extensionId.getGroup(), this.extensionId.getName(), this.extensionId.getVersion());
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ExtensionDependency that = (ExtensionDependency) o;
        return Objects.equals(extensionId, that.extensionId)
                && Objects.equals(conditionalDependencies, that.conditionalDependencies)
                && Objects.equals(dependencyConditions, that.dependencyConditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(extensionId, conditionalDependencies, dependencyConditions);
    }
}
