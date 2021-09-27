package io.quarkus.gradle.dependency;

import java.util.HashSet;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ExternalDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;

public class ApplicationDeploymentClasspathBuilder {

    private static final String DEPLOYMENT_CONFIGURATION_SUFFIX = "Deployment";

    private final Project project;
    private final Set<ExtensionDependency> alreadyProcessed = new HashSet<>();

    public ApplicationDeploymentClasspathBuilder(Project project) {
        this.project = project;
    }

    public static String toDeploymentConfigurationName(String baseConfigurationName) {
        return baseConfigurationName + DEPLOYMENT_CONFIGURATION_SUFFIX;
    }

    public synchronized void createBuildClasspath(Set<ExtensionDependency> extensions, String baseConfigurationName) {
        String deploymentConfigurationName = toDeploymentConfigurationName(baseConfigurationName);
        project.getConfigurations().create(deploymentConfigurationName);

        DependencyHandler dependencies = project.getDependencies();
        for (ExtensionDependency extension : extensions) {
            requireDeploymentDependency(deploymentConfigurationName, extension, dependencies);
            if (alreadyProcessed.contains(extension)) {
                continue;
            }
            alreadyProcessed.add(extension);
            extension.createDeploymentVariant(dependencies);
        }
    }

    private void requireDeploymentDependency(String deploymentConfigurationName, ExtensionDependency extension,
            DependencyHandler dependencies) {
        ExternalDependency dependency = (ExternalDependency) dependencies.add(deploymentConfigurationName,
                extension.asDependencyNotation());
        dependency.capabilities(
                handler -> handler.requireCapability(DependencyUtils.asCapabilityNotation(extension.deploymentModule)));
    }
}
