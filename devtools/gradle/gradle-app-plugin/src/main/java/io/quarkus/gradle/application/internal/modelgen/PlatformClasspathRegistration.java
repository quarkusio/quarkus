package io.quarkus.gradle.application.internal.modelgen;

import java.util.List;

import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.attributes.Category;
import org.gradle.api.plugins.JavaPlugin;

import io.quarkus.bootstrap.BootstrapConstants;

final class PlatformClasspathRegistration {

    PlatformConfigurations register(Project project, String declarationsConfigurationName,
            String platformConfigurationName, String propertiesConfigurationName) {
        NamedDomainObjectProvider<? extends Configuration> platform = registerPlatformConfiguration(project,
                declarationsConfigurationName,
                platformConfigurationName);
        NamedDomainObjectProvider<? extends Configuration> properties = registerPlatformPropertiesConfiguration(project,
                platformConfigurationName,
                propertiesConfigurationName);
        return new PlatformConfigurations(platform, properties);
    }

    private static NamedDomainObjectProvider<? extends Configuration> registerPlatformConfiguration(Project project,
            String declarationsConfigurationName,
            String platformConfigurationName) {
        if (project.getConfigurations().findByName(platformConfigurationName) != null) {
            return project.getConfigurations().named(platformConfigurationName);
        }
        Configuration platformDeclarations = project.getConfigurations()
                .dependencyScope(declarationsConfigurationName, configuration -> configuration
                        .setDescription("Internal declarations of platforms imported by the Quarkus application."))
                .get();
        return project.getConfigurations().resolvable(platformConfigurationName, configuration -> {
            configuration.setCanBeConsumed(false);
            configuration.extendsFrom(platformDeclarations);
            DependencySet implementationDependencies = project.getConfigurations()
                    .getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
                    .getAllDependencies();
            // Mirror platform declarations as they are added or removed. Snapshotting
            // here would miss dependencies contributed by plugins applied later.
            implementationDependencies.all(dependency -> {
                if (dependency instanceof ModuleDependency moduleDependency && isPlatform(moduleDependency)) {
                    platformDeclarations.getDependencies().add(dependency);
                }
            });
            implementationDependencies
                    .whenObjectRemoved(dependency -> platformDeclarations.getDependencies().remove(dependency));
        });
    }

    private static NamedDomainObjectProvider<? extends Configuration> registerPlatformPropertiesConfiguration(Project project,
            String platformConfigurationName, String propertiesConfigurationName) {
        if (project.getConfigurations().findByName(propertiesConfigurationName) != null) {
            return project.getConfigurations().named(propertiesConfigurationName);
        }
        return project.getConfigurations().resolvable(propertiesConfigurationName, configuration -> {
            configuration.setCanBeConsumed(false);
            configuration.setTransitive(false);
            DependencyHandler dependencies = project.getDependencies();
            // Descriptor and properties coordinates are available only after platform
            // resolution; addAllLater keeps that resolution out of plugin application.
            configuration.getDependencies().addAllLater(project.getConfigurations()
                    .getByName(platformConfigurationName)
                    .getIncoming()
                    .getResolutionResult()
                    .getRootComponent()
                    .map(root -> root.getDependencies().stream()
                            .filter(ResolvedDependencyResult.class::isInstance)
                            .map(ResolvedDependencyResult.class::cast)
                            .map(dependency -> dependency.getSelected().getId())
                            .filter(ModuleComponentIdentifier.class::isInstance)
                            .map(ModuleComponentIdentifier.class::cast)
                            .flatMap(platform -> platformArtifactDependencyNotations(platform).stream())
                            .distinct()
                            .sorted()
                            .map(dependencies::create)
                            .toList()));
        });
    }

    private static boolean isPlatform(ModuleDependency dependency) {
        Category category = dependency.getAttributes().getAttribute(Category.CATEGORY_ATTRIBUTE);
        return category != null && (Category.ENFORCED_PLATFORM.equals(category.getName())
                || Category.REGULAR_PLATFORM.equals(category.getName()));
    }

    private static List<String> platformArtifactDependencyNotations(ModuleComponentIdentifier platform) {
        String groupAndArtifact = platform.getGroup() + ":" + platform.getModule();
        String version = platform.getVersion();
        return List.of(
                groupAndArtifact + BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX + ":" + version + ":" + version
                        + "@json",
                groupAndArtifact + BootstrapConstants.PLATFORM_PROPERTIES_ARTIFACT_ID_SUFFIX + ":" + version + "@properties");
    }

    record PlatformConfigurations(
            NamedDomainObjectProvider<? extends Configuration> platform,
            NamedDomainObjectProvider<? extends Configuration> properties) {
    }
}
