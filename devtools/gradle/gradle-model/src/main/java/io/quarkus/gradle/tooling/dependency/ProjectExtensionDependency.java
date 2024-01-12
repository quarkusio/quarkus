package io.quarkus.gradle.tooling.dependency;

import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier;

import io.quarkus.maven.dependency.ArtifactKey;

public class ProjectExtensionDependency extends ExtensionDependency<Project> {
    private final Boolean isIncludedBuild;

    public ProjectExtensionDependency(
            Project extensionProject,
            Project deploymentModule,
            Boolean isIncludedBuild,
            List<Dependency> conditionalDependencies,
            List<ArtifactKey> dependencyConditions) {
        super(DefaultModuleVersionIdentifier.newId(
                extensionProject.getGroup().toString(),
                extensionProject.getName(),
                extensionProject.getVersion().toString()),
                deploymentModule,
                conditionalDependencies,
                dependencyConditions);

        this.isIncludedBuild = isIncludedBuild;
    }

    public Boolean isIncludedBuild() {
        return isIncludedBuild;
    }
}
