package io.quarkus.extension.gradle.dependency;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.gradle.tooling.dependency.DependencyUtils;
import io.quarkus.gradle.tooling.dependency.ExtensionDependency;

public class DeploymentClasspathBuilder {

    private final Project project;
    private final Set<ModuleVersionIdentifier> alreadyProcessed = new HashSet<>();

    public DeploymentClasspathBuilder(Project project) {
        this.project = project;
    }

    public void exportDeploymentClasspath(String configurationName) {

        String deploymentConfigurationName = ToolingUtils.toDeploymentConfigurationName(configurationName);
        project.getConfigurations().create(deploymentConfigurationName, config -> {
            Configuration configuration = DependencyUtils.duplicateConfiguration(project,
                    project.getConfigurations().getByName(configurationName));
            Set<ExtensionDependency<?>> extensionDependencies = collectFirstMetQuarkusExtensions(configuration);

            DependencyHandler dependencies = project.getDependencies();

            for (ExtensionDependency<?> extension : extensionDependencies) {
                if (!alreadyProcessed.add(extension.getExtensionId())) {
                    continue;
                }

                dependencies.add(deploymentConfigurationName,
                        DependencyUtils.createDeploymentDependency(dependencies, extension));
            }
        });
    }

    private Set<ExtensionDependency<?>> collectFirstMetQuarkusExtensions(Configuration configuration) {
        Set<ExtensionDependency<?>> firstLevelExtensions = new HashSet<>();
        Set<ResolvedDependency> firstLevelModuleDependencies = configuration.getResolvedConfiguration()
                .getFirstLevelModuleDependencies();

        Set<ModuleVersionIdentifier> visitedArtifacts = new HashSet<>();
        for (ResolvedDependency firstLevelModuleDependency : firstLevelModuleDependencies) {
            firstLevelExtensions
                    .addAll(collectQuarkusExtensions(firstLevelModuleDependency, visitedArtifacts));
        }
        return firstLevelExtensions;
    }

    private Set<ExtensionDependency<?>> collectQuarkusExtensions(ResolvedDependency dependency,
            Set<ModuleVersionIdentifier> visitedArtifacts) {
        if (visitedArtifacts.contains(dependency.getModule().getId())) {
            return Collections.emptySet();
        } else {
            visitedArtifacts.add(dependency.getModule().getId());
        }
        Set<ExtensionDependency<?>> extensions = new LinkedHashSet<>();
        for (ResolvedArtifact moduleArtifact : dependency.getModuleArtifacts()) {
            ExtensionDependency<?> extension = DependencyUtils.getExtensionInfoOrNull(project, moduleArtifact);
            if (extension != null) {
                extensions.add(extension);
                return extensions;
            }
        }

        for (ResolvedDependency child : dependency.getChildren()) {
            extensions.addAll(collectQuarkusExtensions(child, visitedArtifacts));
        }

        return extensions;
    }
}
