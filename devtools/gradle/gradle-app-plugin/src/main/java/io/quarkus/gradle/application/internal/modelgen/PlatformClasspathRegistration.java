package io.quarkus.gradle.application.internal.modelgen;

import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.Category;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.ListProperty;

import io.quarkus.bootstrap.BootstrapConstants;

final class PlatformClasspathRegistration {

    PlatformConfigurations register(Project project, String declarationsConfigurationName,
            String platformConfigurationName, String propertiesConfigurationName) {
        NamedDomainObjectProvider<? extends Configuration> platform = registerPlatformConfiguration(project,
                declarationsConfigurationName,
                platformConfigurationName);
        NamedDomainObjectProvider<? extends Configuration> properties = registerPlatformPropertiesConfiguration(project,
                platform,
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
            NamedDomainObjectProvider<? extends Configuration> platform, String propertiesConfigurationName) {
        if (project.getConfigurations().findByName(propertiesConfigurationName) != null) {
            return project.getConfigurations().named(propertiesConfigurationName);
        }
        ListProperty<String> platformArtifactDependencies = project.getObjects().listProperty(String.class);
        platform.configure(configuration -> configuration.getResolutionStrategy().eachDependency(dependency -> {
            String artifactId = dependency.getTarget().getName();
            String version = dependency.getTarget().getVersion();
            if (artifactId.endsWith(BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX)) {
                platformArtifactDependencies.add(dependency.getTarget().getGroup() + ":" + artifactId + ":" + version + ":"
                        + version + "@json");
            } else if (artifactId.endsWith(BootstrapConstants.PLATFORM_PROPERTIES_ARTIFACT_ID_SUFFIX)) {
                platformArtifactDependencies
                        .add(dependency.getTarget().getGroup() + ":" + artifactId + ":" + version + "@properties");
            }
        }));
        return project.getConfigurations().resolvable(propertiesConfigurationName, configuration -> {
            configuration.setCanBeConsumed(false);
            configuration.setTransitive(false);
            DependencyHandler dependencies = project.getDependencies();
            // Quarkus platform metadata is published as constraints in a platform BOM.
            // The resolution callback sees those constraints, unlike ordinary Gradle
            // platforms, which prevents deriving Quarkus metadata coordinates for
            // every declared BOM.
            configuration.getDependencies().addAllLater(project.provider(() -> {
                platform.get().getIncoming().getResolutionResult().getRootComponent().get();
                return platformArtifactDependencies.get().stream()
                        .distinct()
                        .sorted()
                        .map(dependencies::create)
                        .toList();
            }));
        });
    }

    private static boolean isPlatform(ModuleDependency dependency) {
        Category category = dependency.getAttributes().getAttribute(Category.CATEGORY_ATTRIBUTE);
        return category != null && (Category.ENFORCED_PLATFORM.equals(category.getName())
                || Category.REGULAR_PLATFORM.equals(category.getName()));
    }

    record PlatformConfigurations(
            NamedDomainObjectProvider<? extends Configuration> platform,
            NamedDomainObjectProvider<? extends Configuration> properties) {
    }
}
