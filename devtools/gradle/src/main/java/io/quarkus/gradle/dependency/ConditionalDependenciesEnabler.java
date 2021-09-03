package io.quarkus.gradle.dependency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;

public class ConditionalDependenciesEnabler {

    private final Map<String, ExtensionDependency> featureVariants = new HashMap<>();

    private final Project project;

    public ConditionalDependenciesEnabler(Project project) {
        this.project = project;
    }

    public void declareConditionalDependencies(String baseConfigurationName) {
        featureVariants.clear();

        Configuration resolvedConfiguration = DependencyUtils.duplicateConfiguration(project,
                project.getConfigurations().getByName(baseConfigurationName));

        Set<ResolvedArtifact> runtimeArtifacts = resolvedConfiguration.getResolvedConfiguration().getResolvedArtifacts();
        List<ExtensionDependency> extensions = collectExtensionsForResolution(runtimeArtifacts);
        featureVariants.putAll(extractFeatureVariants(extensions));

        resolveConditionalDependencies(extensions, resolvedConfiguration, baseConfigurationName);
    }

    private List<ExtensionDependency> collectExtensionsForResolution(Set<ResolvedArtifact> runtimeArtifacts) {
        List<ExtensionDependency> firstLevelExtensions = new ArrayList<>();
        for (ResolvedArtifact artifact : runtimeArtifacts) {
            ExtensionDependency extension = DependencyUtils.getExtensionInfoOrNull(project, artifact);
            if (extension != null) {
                if (!extension.conditionalDependencies.isEmpty()) {
                    if (extension.needsResolution(runtimeArtifacts)) {
                        firstLevelExtensions.add(extension);
                    }
                }
            }
        }
        return firstLevelExtensions;
    }

    private void resolveConditionalDependencies(List<ExtensionDependency> conditionalExtensions,
            Configuration existingDependencies, String baseConfigurationName) {
        final Configuration conditionalDeps = createConditionalDependenciesConfiguration(existingDependencies,
                conditionalExtensions);
        boolean hasChanged = false;
        List<ExtensionDependency> newConditionalDependencies = new ArrayList<>();
        newConditionalDependencies.addAll(conditionalExtensions);
        for (ResolvedArtifact artifact : conditionalDeps.getResolvedConfiguration().getResolvedArtifacts()) {
            ExtensionDependency extensionDependency = DependencyUtils.getExtensionInfoOrNull(project, artifact);
            if (extensionDependency != null) {
                if (DependencyUtils.exist(conditionalDeps.getResolvedConfiguration().getResolvedArtifacts(),
                        extensionDependency.dependencyConditions)) {
                    enableConditionalDependency(extensionDependency.extensionId);
                    if (!extensionDependency.conditionalDependencies.isEmpty()) {
                        featureVariants.putAll(extractFeatureVariants(Collections.singletonList(extensionDependency)));
                    }
                }
                if (!conditionalExtensions.contains(extensionDependency)) {
                    hasChanged = true;
                    newConditionalDependencies.add(extensionDependency);
                }
            }
        }

        Configuration enhancedDependencies = DependencyUtils.duplicateConfiguration(project,
                project.getConfigurations().getByName(baseConfigurationName));

        if (hasChanged) {
            if (!newConditionalDependencies.isEmpty()) {
                resolveConditionalDependencies(newConditionalDependencies, enhancedDependencies, baseConfigurationName);
            }
        }
    }

    private Map<String, ExtensionDependency> extractFeatureVariants(List<ExtensionDependency> extensions) {
        Map<String, ExtensionDependency> possibleVariant = new HashMap<>();
        for (ExtensionDependency extension : extensions) {
            for (Dependency dependency : extension.conditionalDependencies) {
                possibleVariant.put(DependencyUtils.asFeatureName(dependency), extension);
            }
        }
        return possibleVariant;
    }

    private Configuration createConditionalDependenciesConfiguration(Configuration existingDeps,
            List<ExtensionDependency> extensions) {
        Configuration newConfiguration = existingDeps.copy();
        newConfiguration.getDependencies().addAll(collectConditionalDependencies(extensions));
        return newConfiguration;
    }

    private Set<Dependency> collectConditionalDependencies(List<ExtensionDependency> extensionDependencies) {
        Set<Dependency> dependencies = new HashSet<>();
        for (ExtensionDependency extensionDependency : extensionDependencies) {
            dependencies.add(extensionDependency.asDependency(project.getDependencies()));
            dependencies.addAll(extensionDependency.conditionalDependencies);
        }
        return dependencies;
    }

    private void enableConditionalDependency(ModuleVersionIdentifier dependency) {
        ExtensionDependency extension = featureVariants.get(DependencyUtils.asFeatureName(dependency));
        if (extension == null) {
            return;
        }
        extension.importConditionalDependency(project.getDependencies(), dependency);
    }
}
