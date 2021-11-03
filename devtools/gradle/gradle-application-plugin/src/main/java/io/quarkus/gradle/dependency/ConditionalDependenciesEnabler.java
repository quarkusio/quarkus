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
    private final Set<ExtensionDependency> allExtensions = new HashSet<>();
    private final Project project;

    public ConditionalDependenciesEnabler(Project project) {
        this.project = project;
    }

    public Set<ExtensionDependency> declareConditionalDependencies(String baseConfigurationName) {
        featureVariants.clear();
        allExtensions.clear();
        Configuration baseConfiguration = project.getConfigurations().getByName(baseConfigurationName);
        if (baseConfiguration.getIncoming().getDependencies().isEmpty()) {
            return Collections.emptySet();
        }
        Configuration resolvedConfiguration = DependencyUtils.duplicateConfiguration(project, baseConfiguration);
        Set<ResolvedArtifact> runtimeArtifacts = resolvedConfiguration.getResolvedConfiguration().getResolvedArtifacts();

        List<ExtensionDependency> extensions = collectExtensionsForResolution(runtimeArtifacts);
        if (extensions.isEmpty()) {
            return allExtensions;
        }

        featureVariants.putAll(extractFeatureVariants(extensions));

        resolveConditionalDependencies(extensions, runtimeArtifacts, baseConfigurationName);
        return allExtensions;
    }

    private List<ExtensionDependency> collectExtensionsForResolution(Set<ResolvedArtifact> runtimeArtifacts) {
        List<ExtensionDependency> firstLevelExtensions = new ArrayList<>();
        for (ResolvedArtifact artifact : runtimeArtifacts) {
            ExtensionDependency extension = DependencyUtils.getExtensionInfoOrNull(project, artifact);
            if (extension != null) {
                allExtensions.add(extension);
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
            Set<ResolvedArtifact> runtimeArtifacts, String baseConfigurationName) {

        boolean hasChanged = false;
        List<ExtensionDependency> newConditionalDependencies = new ArrayList<>();

        final Configuration conditionalDeps = createConditionalDependenciesConfiguration(project,
                conditionalExtensions);
        Set<ResolvedArtifact> resolvedArtifacts = conditionalDeps.getResolvedConfiguration().getResolvedArtifacts();

        Set<ResolvedArtifact> availableRuntimeArtifacts = new HashSet<>();
        availableRuntimeArtifacts.addAll(runtimeArtifacts);
        availableRuntimeArtifacts.addAll(resolvedArtifacts);

        for (ResolvedArtifact artifact : resolvedArtifacts) {
            ExtensionDependency extensionDependency = DependencyUtils.getExtensionInfoOrNull(project, artifact);
            if (extensionDependency != null) {
                if (DependencyUtils.exist(availableRuntimeArtifacts,
                        extensionDependency.dependencyConditions)) {
                    enableConditionalDependency(extensionDependency.extensionId);
                    extensionDependency.setConditional(true);
                    allExtensions.add(extensionDependency);
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

        if (hasChanged) {
            if (!newConditionalDependencies.isEmpty()) {
                Configuration enhancedDependencies = DependencyUtils.duplicateConfiguration(project,
                        project.getConfigurations().getByName(baseConfigurationName));
                Set<ResolvedArtifact> enhancedRuntimeArtifacts = enhancedDependencies.getResolvedConfiguration()
                        .getResolvedArtifacts();
                resolveConditionalDependencies(newConditionalDependencies, enhancedRuntimeArtifacts, baseConfigurationName);
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

    private Configuration createConditionalDependenciesConfiguration(Project project,
            List<ExtensionDependency> extensions) {
        Set<Dependency> dependencies = collectConditionalDependencies(extensions);
        return project.getConfigurations().detachedConfiguration(dependencies.toArray(new Dependency[0]));
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
