package io.quarkus.gradle.application.internal.modelgen;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.artifacts.result.ResolvedVariantResult;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.attributes.java.TargetJvmEnvironment;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;

import io.quarkus.gradle.model.config.ExtensionVariantConstants;
import io.quarkus.maven.dependency.ArtifactCoords;

final class LocalExtensionDependencyRegistration {

    private final Project project;

    LocalExtensionDependencyRegistration(Project project) {
        this.project = project;
    }

    Configuration registerConditionalDevDependenciesConfiguration(String configurationName,
            Configuration... rawRuntimeConfigurations) {
        if (project.getConfigurations().findByName(configurationName) == null) {
            project.getConfigurations().resolvable(configurationName, configuration -> {
                configuration.setCanBeConsumed(false);
                setJavaRuntimeAttributes(configuration.getAttributes());
                configuration.getDependencies().addAllLater(variantDependencies(
                        ExtensionVariantConstants.EXTENSION_CONDITIONAL_DEV_DEPENDENCIES_CATEGORY,
                        ExtensionVariantConstants.EXTENSION_CONDITIONAL_DEV_DEPENDENCIES_ATTRIBUTE,
                        rawRuntimeConfigurations));
            });
        }
        return project.getConfigurations().getByName(configurationName);
    }

    Provider<List<String>> conditionalDevDependencyCoordinates(Configuration configuration) {
        return configuration.getIncoming().getResolutionResult().getRootComponent()
                .map(root -> {
                    Set<String> coordinates = new TreeSet<>();
                    for (DependencyResult dependency : root.getDependencies()) {
                        if (!(dependency instanceof ResolvedDependencyResult resolved)
                                || !selectedVariant(resolved.getSelected(),
                                        ExtensionVariantConstants.EXTENSION_CONDITIONAL_DEV_DEPENDENCIES_ATTRIBUTE)) {
                            continue;
                        }
                        for (DependencyResult conditionalDependency : resolved.getSelected().getDependencies()) {
                            if (conditionalDependency instanceof ResolvedDependencyResult conditionalResolved
                                    && conditionalResolved.getSelected().getId() instanceof ModuleComponentIdentifier module) {
                                coordinates.add(module.getGroup() + ":" + module.getModule() + ":" + module.getVersion());
                            }
                        }
                    }
                    return coordinates.stream().toList();
                });
    }

    Provider<List<Dependency>> variantDependencies(
            String variantName, Attribute<Boolean> attribute, Configuration... configurations) {
        DependencyHandler dependencies = project.getDependencies();
        ObjectFactory objects = project.getObjects();
        String currentBuildPath = project.getGradle().getBuildPath();
        // Resolution providers carry only serialized identities. Recreate Gradle
        // dependencies afterward instead of retaining resolved model objects.
        return componentSpecs(configurations)
                .map(serializedSpecs -> serializedSpecs.stream()
                        .map(LocalExtensionComponentSpec::deserialize)
                        .map(spec -> (Dependency) variantDependency(
                                dependencies, objects, currentBuildPath, spec, variantName, attribute))
                        .toList());
    }

    Provider<Iterable<String>> deploymentDependencySpecs(Configuration configuration) {
        String currentBuildPath = project.getGradle().getBuildPath();
        return componentSpecs(configuration)
                .map(serializedSpecs -> serializedSpecs.stream()
                        .map(LocalExtensionComponentSpec::deserialize)
                        // ProjectDependency can address only the current build.
                        // Included builds must use substituted module coordinates.
                        .map(spec -> spec.sameBuild(currentBuildPath)
                                ? DeploymentDependencySpec.project(spec.projectPath(), requiredModuleNotation(spec))
                                : DeploymentDependencySpec.includedProject(requiredModuleNotation(spec)))
                        .map(DeploymentDependencySpec::serialize)
                        .toList());
    }

    Dependency deploymentDependency(DependencyHandler dependencies, ObjectFactory objects, String serializedSpec) {
        DeploymentDependencySpec spec = DeploymentDependencySpec.deserialize(serializedSpec);
        if (spec.external()) {
            return dependencies.create(spec.value());
        }
        ModuleDependency dependency = spec.project()
                ? (ProjectDependency) dependencies.project(Map.of("path", spec.value()))
                : (ModuleDependency) dependencies.create(spec.value());
        dependency.attributes(attributes -> {
            attributes.attribute(Category.CATEGORY_ATTRIBUTE,
                    objects.named(Category.class, Category.LIBRARY));
            attributes.attribute(ExtensionVariantConstants.EXTENSION_DEPLOYMENT_DEPENDENCY_ATTRIBUTE, true);
        });
        dependency.capabilities(capabilities -> capabilities.requireCapability(
                extensionVariantCapability(spec.moduleNotation(),
                        ExtensionVariantConstants.EXTENSION_DEPLOYMENT_DEPENDENCY_CATEGORY)));
        return dependency;
    }

    private Provider<List<String>> componentSpecs(Configuration... configurations) {
        Provider<List<String>> componentSpecs = project.provider(List::of);
        for (Configuration configuration : configurations) {
            Provider<List<String>> configurationComponentSpecs = configuration.getIncoming().getResolutionResult()
                    .getRootComponent().map(root -> {
                        Set<ComponentIdentifier> visited = new HashSet<>();
                        Set<LocalExtensionComponentSpec> specs = new TreeSet<>();
                        collectComponentSpecs(root, visited, specs);
                        return specs.stream().map(LocalExtensionComponentSpec::serialize).toList();
                    });
            // A project can occur through several classpaths or graph paths. Stable
            // identity ordering makes the merged dependency set deterministic.
            componentSpecs = componentSpecs.zip(configurationComponentSpecs, (existing, additional) -> {
                Set<String> merged = new TreeSet<>(existing);
                merged.addAll(additional);
                return merged.stream().toList();
            });
        }
        return componentSpecs;
    }

    private static void collectComponentSpecs(
            ResolvedComponentResult component, Set<ComponentIdentifier> visited,
            Set<LocalExtensionComponentSpec> componentSpecs) {
        if (!visited.add(component.getId())) {
            return;
        }
        if (component.getId() instanceof ProjectComponentIdentifier projectComponent
                && selectedVariant(component, ExtensionVariantConstants.EXTENSION_RUNTIME_ATTRIBUTE)) {
            componentSpecs.add(new LocalExtensionComponentSpec(
                    projectComponent.getBuild().getBuildPath(),
                    projectComponent.getProjectPath(),
                    moduleNotation(component)));
        }
        for (DependencyResult dependency : component.getDependencies()) {
            if (dependency instanceof ResolvedDependencyResult resolved) {
                collectComponentSpecs(resolved.getSelected(), visited, componentSpecs);
            }
        }
    }

    private static String moduleNotation(ResolvedComponentResult component) {
        for (ResolvedDependencyResult dependent : component.getDependents()) {
            if (dependent.getRequested() instanceof ModuleComponentSelector module) {
                String version = module.getVersion();
                if (!version.isBlank()) {
                    return module.getGroup() + ":" + module.getModule() + ":" + version;
                }
            }
        }
        ModuleVersionIdentifier moduleVersion = component.getModuleVersion();
        return moduleVersion == null ? ""
                : moduleVersion.getGroup() + ":" + moduleVersion.getName() + ":" + moduleVersion.getVersion();
    }

    private static String requiredModuleNotation(LocalExtensionComponentSpec spec) {
        if (!spec.moduleNotation().isBlank()) {
            return spec.moduleNotation();
        }
        throw new GradleException("Included-build Quarkus extension project '" + spec.buildPath() + spec.projectPath()
                + "' cannot be addressed through module coordinates. Declare an includeBuild dependency substitution "
                + "for the extension runtime project.");
    }

    static boolean selectedVariant(ResolvedComponentResult component, Attribute<Boolean> attribute) {
        for (ResolvedVariantResult variant : component.getVariants()) {
            if (Boolean.TRUE.equals(variant.getAttributes().getAttribute(attribute))) {
                return true;
            }
        }
        return false;
    }

    private static ModuleDependency variantDependency(DependencyHandler dependencies, ObjectFactory objects,
            String currentBuildPath, LocalExtensionComponentSpec spec, String variantName, Attribute<Boolean> attribute) {
        // Same-build components retain project identity. Composite-build components
        // are selected through their substituted module coordinates instead.
        ModuleDependency dependency = spec.sameBuild(currentBuildPath)
                ? (ProjectDependency) dependencies.project(Map.of("path", spec.projectPath()))
                : (ModuleDependency) dependencies.create(requiredModuleNotation(spec));
        dependency.attributes(attributes -> {
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.LIBRARY));
            attributes.attribute(attribute, true);
        });
        dependency.capabilities(capabilities -> capabilities.requireCapability(
                extensionVariantCapability(spec.moduleNotation(), variantName)));
        return dependency;
    }

    private static String extensionVariantCapability(String moduleNotation, String variantName) {
        ArtifactCoords coordinates = ArtifactCoords.fromString(moduleNotation);
        return ExtensionVariantConstants.extensionVariantCapability(
                coordinates.getGroupId(), coordinates.getArtifactId(), coordinates.getVersion(), variantName);
    }

    private void setJavaRuntimeAttributes(AttributeContainer attributes) {
        ObjectFactory objects = project.getObjects();
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.LIBRARY));
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, Usage.JAVA_RUNTIME));
        attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                objects.named(LibraryElements.class, LibraryElements.JAR));
        attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.class, Bundling.EXTERNAL));
        attributes.attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                objects.named(TargetJvmEnvironment.class, TargetJvmEnvironment.STANDARD_JVM));
    }
}
