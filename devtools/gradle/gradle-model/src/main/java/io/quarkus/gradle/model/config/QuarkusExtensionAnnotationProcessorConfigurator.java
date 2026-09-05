package io.quarkus.gradle.model.config;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.attributes.Category;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.ListProperty;

public final class QuarkusExtensionAnnotationProcessorConfigurator {

    private static final String QUARKUS_CORE_GROUP = "io.quarkus";
    private static final String QUARKUS_CORE_NAME = "quarkus-core";
    private static final String QUARKUS_ANNOTATION_PROCESSOR = "io.quarkus:quarkus-extension-processor";
    private static final Set<String> QUARKUS_PLATFORM_GROUPS = Set.of("io.quarkus", "io.quarkus.platform");

    private final Function<Project, String> quarkusCoreVersionResolver;

    public QuarkusExtensionAnnotationProcessorConfigurator() {
        this(QuarkusExtensionAnnotationProcessorConfigurator::resolveQuarkusCoreVersionFromCompileClasspath);
    }

    public QuarkusExtensionAnnotationProcessorConfigurator(Function<Project, String> quarkusCoreVersionResolver) {
        this.quarkusCoreVersionResolver = quarkusCoreVersionResolver;
    }

    public void configure(Project project) {
        DependencySet annotationProcessorDependencies = project.getConfigurations()
                .getByName(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME)
                .getDependencies();
        annotationProcessorDependencies.addAllLater(quarkusPlatformDependencies(project));
        annotationProcessorDependencies.addAllLater(annotationProcessorDependency(project));
    }

    private ListProperty<Dependency> quarkusPlatformDependencies(Project project) {
        ListProperty<Dependency> dependencyListProperty = project.getObjects().listProperty(Dependency.class);
        return dependencyListProperty.value(project.provider(() -> project.getConfigurations()
                .getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
                .getAllDependencies()
                .stream()
                .filter(QuarkusExtensionAnnotationProcessorConfigurator::isQuarkusPlatform)
                .map(Dependency::copy)
                .collect(Collectors.toList())));
    }

    private ListProperty<Dependency> annotationProcessorDependency(Project project) {
        ListProperty<Dependency> dependencyListProperty = project.getObjects().listProperty(Dependency.class);
        return dependencyListProperty.value(project.provider(() -> {
            if (hasQuarkusPlatform(project)) {
                return List.of(project.getDependencies().create(QUARKUS_ANNOTATION_PROCESSOR));
            }

            project.getLogger().debug(
                    "No Quarkus platform dependency found; resolving the compile classpath to determine the quarkus-core version.");
            String quarkusCoreVersion = quarkusCoreVersionResolver.apply(project);
            if (quarkusCoreVersion != null && !quarkusCoreVersion.isEmpty()) {
                return List.of(project.getDependencies().create(QUARKUS_ANNOTATION_PROCESSOR + ':' + quarkusCoreVersion));
            }
            return List.of();
        }));
    }

    private boolean hasQuarkusPlatform(Project project) {
        return project.getConfigurations()
                .getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
                .getAllDependencies()
                .stream()
                .anyMatch(QuarkusExtensionAnnotationProcessorConfigurator::isQuarkusPlatform);
    }

    private static boolean isQuarkusPlatform(Dependency dependency) {
        return dependency instanceof ModuleDependency moduleDependency
                && isEnforcedPlatform(moduleDependency)
                && QUARKUS_PLATFORM_GROUPS.contains(dependency.getGroup());
    }

    private static boolean isEnforcedPlatform(ModuleDependency module) {
        final Category category = module.getAttributes().getAttribute(Category.CATEGORY_ATTRIBUTE);
        return category != null && (Category.ENFORCED_PLATFORM.equals(category.getName())
                || Category.REGULAR_PLATFORM.equals(category.getName()));
    }

    private static String resolveQuarkusCoreVersionFromCompileClasspath(Project project) {
        Configuration compileClasspath = project.getConfigurations()
                .getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME);
        Configuration compileClasspathCopy = project.getConfigurations()
                .detachedConfiguration(compileClasspath.getAllDependencies()
                        .stream()
                        .map(Dependency::copy)
                        .toArray(Dependency[]::new));
        compileClasspathCopy.getDependencyConstraints().addAll(compileClasspath.getAllDependencyConstraints());
        Set<ResolvedArtifact> compileClasspathArtifacts = compileClasspathCopy
                .getResolvedConfiguration()
                .getResolvedArtifacts();

        for (ResolvedArtifact artifact : compileClasspathArtifacts) {
            ModuleVersionIdentifier id = artifact.getModuleVersion().getId();
            if (QUARKUS_CORE_GROUP.equals(id.getGroup()) && QUARKUS_CORE_NAME.equals(id.getName())
                    && !id.getVersion().isEmpty()) {
                return id.getVersion();
            }
        }
        return null;
    }
}
