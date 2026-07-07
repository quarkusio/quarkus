package io.quarkus.gradle.application.internal.plugin;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.TaskProvider;

import io.quarkus.gradle.application.internal.planning.TaskNameSegment;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildDescriptor;
import io.quarkus.gradle.application.model.QuarkusApplicationVariantAttributes;
import io.quarkus.gradle.application.tasks.QuarkusApplicationBuildTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationPackageTask;

final class NamedPackageVariantRegistration {

    NamedPackageVariants register(Project project, QuarkusApplicationBuildDescriptor descriptor,
            TaskProvider<? extends QuarkusApplicationBuildTask> namedBuild) {
        ConfigurationContainer configurations = project.getConfigurations();
        ArtifactHandler artifacts = project.getArtifacts();
        ObjectFactory objects = project.getObjects();
        String packageConfigurationName = packageElementsConfigurationName(descriptor.name());
        configurations.register(packageConfigurationName, configuration -> {
            configuration.setDescription("Provides the complete relocatable package directory produced by the '"
                    + descriptor.name() + "' Quarkus application package build.");
            configuration.setCanBeConsumed(true);
            configuration.setCanBeResolved(false);
            configuration.setCanBeDeclared(false);
            configuration.getAttributes().attribute(Category.CATEGORY_ATTRIBUTE,
                    objects.named(Category.class, QuarkusApplicationVariantAttributes.PACKAGE_CATEGORY));
            configuration.getAttributes().attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                    objects.named(LibraryElements.class,
                            QuarkusApplicationVariantAttributes.PACKAGE_LIBRARY_ELEMENTS));
            configuration.getAttributes().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                    ArtifactTypeDefinition.DIRECTORY_TYPE);
            configuration.getAttributes().attribute(QuarkusApplicationVariantAttributes.BUILD_NAME_ATTRIBUTE,
                    descriptor.name());
            configuration.getAttributes().attribute(QuarkusApplicationVariantAttributes.BUILD_TYPE_ATTRIBUTE,
                    descriptor.type().jarType().orElse(descriptor.type().name()));
            artifacts.add(packageConfigurationName,
                    namedBuild.flatMap(QuarkusApplicationBuildTask::getOutputDirectory),
                    artifact -> {
                        artifact.setType(ArtifactTypeDefinition.DIRECTORY_TYPE);
                        artifact.builtBy(namedBuild);
                    });
        });

        String launcherConfigurationName = launcherJarElementsConfigurationName(descriptor.name());
        configurations.register(launcherConfigurationName, configuration -> {
            configuration.setDescription("Provides the primary producer-local launcher JAR produced by the '"
                    + descriptor.name()
                    + "' Quarkus application package build; layout launchers may require sibling package files.");
            configuration.setCanBeConsumed(true);
            configuration.setCanBeResolved(false);
            configuration.setCanBeDeclared(false);
            configuration.getAttributes().attribute(Category.CATEGORY_ATTRIBUTE,
                    objects.named(Category.class, QuarkusApplicationVariantAttributes.LAUNCHER_CATEGORY));
            configuration.getAttributes().attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                    objects.named(LibraryElements.class,
                            QuarkusApplicationVariantAttributes.LAUNCHER_LIBRARY_ELEMENTS));
            configuration.getAttributes().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                    ArtifactTypeDefinition.JAR_TYPE);
            configuration.getAttributes().attribute(QuarkusApplicationVariantAttributes.BUILD_NAME_ATTRIBUTE,
                    descriptor.name());
            configuration.getAttributes().attribute(QuarkusApplicationVariantAttributes.BUILD_TYPE_ATTRIBUTE,
                    descriptor.type().jarType().orElse(descriptor.type().name()));
            artifacts.add(launcherConfigurationName,
                    namedBuild.flatMap(task -> ((QuarkusApplicationPackageTask) task).getPrimaryJarFile()),
                    artifact -> {
                        artifact.setType(ArtifactTypeDefinition.JAR_TYPE);
                        artifact.builtBy(namedBuild);
                    });
        });
        return new NamedPackageVariants(packageConfigurationName, launcherConfigurationName);
    }

    static String packageElementsConfigurationName(String buildName) {
        return "quarkus" + TaskNameSegment.of(buildName).value() + "PackageElements";
    }

    static String launcherJarElementsConfigurationName(String buildName) {
        return "quarkus" + TaskNameSegment.of(buildName).value() + "LauncherJarElements";
    }

    record NamedPackageVariants(String packageElementsConfiguration, String launcherJarElementsConfiguration) {
    }
}
