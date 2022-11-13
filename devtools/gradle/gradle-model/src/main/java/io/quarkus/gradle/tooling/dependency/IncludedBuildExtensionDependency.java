package io.quarkus.gradle.tooling.dependency;

import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;

public class IncludedBuildExtensionDependency extends LocalExtensionDependency {
    public IncludedBuildExtensionDependency(Project localProject, ModuleVersionIdentifier extensionId,
            ArtifactCoords deploymentModule,
            List<Dependency> conditionalDependencies, List<ArtifactKey> dependencyConditions) {
        super(localProject, extensionId, deploymentModule, conditionalDependencies, dependencyConditions);
    }

    public Dependency getDeployment() {
        return new DefaultExternalModuleDependency(deploymentModule.getGroupId(), deploymentModule.getArtifactId(),
                deploymentModule.getVersion());
    }
}
