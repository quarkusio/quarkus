package io.quarkus.gradle.dependency;

import java.util.HashSet;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ExternalDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;

public class ApplicationDeploymentClasspathBuilder {

    private static final String DEPLOYMENT_CONFIGURATION_SUFFIX = "Deployment";

    private final Project project;
    private final Set<ExtensionDependency> commonExtensions = new HashSet<>();

    public ApplicationDeploymentClasspathBuilder(Project project) {
        this.project = project;
    }

    public static String toDeploymentConfigurationName(String baseConfigurationName) {
        return baseConfigurationName + DEPLOYMENT_CONFIGURATION_SUFFIX;
    }

    public void createBuildClasspath(Set<ExtensionDependency> extensions, String baseConfigurationName, boolean common) {
        String deploymentConfigurationName = toDeploymentConfigurationName(baseConfigurationName);
        project.getConfigurations().create(deploymentConfigurationName);

        DependencyHandler dependencies = project.getDependencies();
        for (ExtensionDependency extension : extensions) {
            if (common) {
                commonExtensions.add(extension);
            } else if (commonExtensions.contains(extension)) {
                continue;
            }
            extension.createDeploymentVariant(dependencies);
            requireDeploymentDependency(deploymentConfigurationName, extension, dependencies);
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
