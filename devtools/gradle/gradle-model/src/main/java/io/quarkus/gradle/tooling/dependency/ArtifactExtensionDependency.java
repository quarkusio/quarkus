package io.quarkus.gradle.tooling.dependency;

import java.util.List;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;

public class ArtifactExtensionDependency extends ExtensionDependency<ArtifactCoords> {
    public ArtifactExtensionDependency(ModuleVersionIdentifier extensionId,
            ArtifactCoords deploymentModule,
            List<Dependency> conditionalDependencies,
            List<ArtifactKey> dependencyConditions) {
        super(extensionId, deploymentModule, conditionalDependencies, dependencyConditions);
    }
}
