package io.quarkus.gradle.dependency;

import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;

import io.quarkus.runtime.LaunchMode;

/**
 * Registers and initializes project's build time (deployment) classpath configuration
 */
public class DeploymentConfigurationResolver {

    /**
     * Registers and initializes project's build time (deployment) classpath configuration for a given project,
     * launch mode and direct deployment dependencies provider.
     * <p/>
     * The configuration will re-use component variants added previously by the {@link QuarkusComponentVariants}.
     * <p/>
     * Deployment dependencies that could not be added using variants, will be added as direct dependencies
     * of the configuration.
     *
     * @param project project
     * @param mode launch mode
     * @param configurationName configuration name
     * @param directDeploymentDeps direct deployment dependencies provider
     */
    public static void registerDeploymentConfiguration(Project project, LaunchMode mode, String configurationName,
            Provider<List<Dependency>> directDeploymentDeps) {
        project.getConfigurations().register(configurationName,
                config -> new DeploymentConfigurationResolver(project, config, mode, directDeploymentDeps));
    }

    private DeploymentConfigurationResolver(Project project, Configuration deploymentConfig, LaunchMode mode,
            Provider<List<Dependency>> directDeploymentDeps) {
        final Configuration baseRuntimeConfig = project.getConfigurations()
                .getByName(ApplicationDeploymentClasspathBuilder.getFinalRuntimeConfigName(mode));
        deploymentConfig.setCanBeConsumed(false);
        deploymentConfig.extendsFrom(baseRuntimeConfig);
        deploymentConfig.shouldResolveConsistentlyWith(baseRuntimeConfig);

        ListProperty<Dependency> dependencyListProperty = project.getObjects().listProperty(Dependency.class);
        deploymentConfig.getDependencies().addAllLater(
                dependencyListProperty.value(directDeploymentDeps));
        QuarkusComponentVariants.setDeploymentAndConditionalAttributes(deploymentConfig, project, mode);
    }
}
