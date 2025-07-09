package io.quarkus.gradle.dependency;

import java.util.ArrayList;
import java.util.Collection;
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

import io.quarkus.gradle.tooling.dependency.DependencyUtils;
import io.quarkus.gradle.tooling.dependency.ExtensionDependency;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.runtime.LaunchMode;

public class ConditionalDependenciesEnabler {

    /**
     * Links dependencies to extensions
     */
    private final Map<ArtifactKey, Set<ExtensionDependency<?>>> featureVariants = new HashMap<>();
    /**
     * Despite its name, only contains extensions which have no conditional dependencies, or have
     * resolved their conditional dependencies.
     */
    private final Map<ModuleVersionIdentifier, ExtensionDependency<?>> allExtensions = new HashMap<>();
    private final Project project;
    private final Configuration enforcedPlatforms;
    private final Set<ArtifactKey> existingArtifacts = new HashSet<>();
    private final List<Dependency> unsatisfiedConditionalDeps = new ArrayList<>();

    public ConditionalDependenciesEnabler(Project project, LaunchMode mode,
            Configuration platforms) {
        this.project = project;
        this.enforcedPlatforms = platforms;

        // Get runtimeClasspath (quarkusProdBaseRuntimeClasspathConfiguration to be exact)
        Configuration baseRuntimeConfig = project.getConfigurations()
                .getByName(ApplicationDeploymentClasspathBuilder.getBaseRuntimeConfigName(mode));

        if (!baseRuntimeConfig.getIncoming().getDependencies().isEmpty()) {
            // Gather all extensions from the full resolved dependency tree
            collectConditionalDependencies(baseRuntimeConfig.getResolvedConfiguration().getResolvedArtifacts(), mode);
            // If there are any extensions which had unresolved conditional dependencies:
            while (!unsatisfiedConditionalDeps.isEmpty()) {
                boolean satisfiedConditionalDeps = false;
                final int originalUnsatisfiedCount = unsatisfiedConditionalDeps.size();
                int i = 0;
                // Go through each unsatisfied/unresolved dependency once:
                while (i < unsatisfiedConditionalDeps.size()) {
                    final Dependency conditionalDep = unsatisfiedConditionalDeps.get(i);
                    // Try to resolve it with the latest evolved graph available
                    if (resolveConditionalDependency(conditionalDep, mode)) {
                        // Mark the resolution as a success so we know the graph evolved
                        satisfiedConditionalDeps = true;
                        unsatisfiedConditionalDeps.remove(i);
                    } else {
                        // No resolution (yet) or graph evolution; move on to the next
                        ++i;
                    }
                }
                // If we didn't resolve any dependencies and the graph did not evolve, give up.
                if (!satisfiedConditionalDeps && unsatisfiedConditionalDeps.size() == originalUnsatisfiedCount) {
                    break;
                }
            }
            reset();
        }
    }

    public Collection<ExtensionDependency<?>> getAllExtensions() {
        return allExtensions.values();
    }

    private void reset() {
        featureVariants.clear();
        existingArtifacts.clear();
        unsatisfiedConditionalDeps.clear();
    }

    private void collectConditionalDependencies(Set<ResolvedArtifact> runtimeArtifacts, LaunchMode mode) {
        // For every artifact in the dependency graph:
        for (ResolvedArtifact artifact : runtimeArtifacts) {
            // Add to master list of artifacts:
            existingArtifacts.add(getKey(artifact));
            ExtensionDependency<?> extension = DependencyUtils.getExtensionInfoOrNull(project, artifact);
            // If this artifact represents an extension:
            if (extension != null) {
                // Add to master list of accepted extensions:
                allExtensions.put(extension.getExtensionId(), extension);
                queueConditionalDependencies(extension, mode);
            }
        }
    }

    private void queueConditionalDependencies(ExtensionDependency<?> extension, LaunchMode mode) {
        queueConditionalDependencies(extension, extension.getConditionalDependencies());
        if (LaunchMode.DEVELOPMENT.equals(mode)) {
            queueConditionalDependencies(extension, extension.getConditionalDevDependencies());
        }
    }

    private void queueConditionalDependencies(ExtensionDependency<?> extension, Collection<Dependency> conditionalDeps) {
        for (Dependency conditionalDep : conditionalDeps) {
            // If the dependency is not present yet in the graph, queue it for resolution later
            if (!exists(conditionalDep)) {
                queueConditionalDependency(extension, conditionalDep);
            }
        }
    }

    private boolean resolveConditionalDependency(Dependency conditionalDep, LaunchMode mode) {

        final Configuration conditionalDeps = createConditionalDependenciesConfiguration(project, conditionalDep);
        Set<ResolvedArtifact> resolvedArtifacts = conditionalDeps.getResolvedConfiguration().getResolvedArtifacts();

        boolean satisfied = false;
        // Resolved artifacts don't have great linking back to the original artifact, so I think
        // this loop is trying to find the artifact that represents the original conditional
        // dependency
        for (ResolvedArtifact artifact : resolvedArtifacts) {
            if (conditionalDep.getName().equals(artifact.getName())
                    && conditionalDep.getVersion().equals(artifact.getModuleVersion().getId().getVersion())
                    && artifact.getModuleVersion().getId().getGroup().equals(conditionalDep.getGroup())) {
                // Once the dependency is found, reload the extension info from within
                final ExtensionDependency<?> extensionDependency = DependencyUtils.getExtensionInfoOrNull(project, artifact);
                // Now check if this conditional dependency is resolved given the latest graph evolution
                if (extensionDependency == null ||
                        (extensionDependency.getDependencyConditions().isEmpty() ||
                                exist(extensionDependency.getDependencyConditions()))) {
                    satisfied = true;
                    enableConditionalDependency(artifact.getModuleVersion().getId());
                    break;
                }
            }
        }

        // No resolution (yet); give up.
        if (!satisfied) {
            return false;
        }

        // The conditional dependency resolved! Let's now add all of /its/ dependencies
        for (ResolvedArtifact artifact : resolvedArtifacts) {
            // First add the artifact to the master list
            existingArtifacts.add(getKey(artifact));
            ExtensionDependency<?> extensionDependency = DependencyUtils.getExtensionInfoOrNull(project, artifact);
            if (extensionDependency == null) {
                continue;
            }
            // If this artifact represents an extension, mark this one as a conditional extension
            extensionDependency.setConditional(true);
            // Add to the master list of accepted extensions
            allExtensions.put(extensionDependency.getExtensionId(), extensionDependency);
            queueConditionalDependencies(extensionDependency, mode);
        }
        return satisfied;
    }

    private void queueConditionalDependency(ExtensionDependency<?> extension, Dependency conditionalDep) {
        // 1. Add to master list of unresolved/unsatisfied dependencies
        // 2. Add map entry to link dependency to extension
        featureVariants.computeIfAbsent(getFeatureKey(conditionalDep), k -> {
            unsatisfiedConditionalDeps.add(conditionalDep);
            return new HashSet<>();
        }).add(extension);
    }

    private Configuration createConditionalDependenciesConfiguration(Project project, Dependency conditionalDep) {
        // Ideally, we want a named configuration here to make sure platform version constraints are enforced
        // on conditional dependencies (https://github.com/gradle/gradle/issues/6881)
        // However, if we use a named configuration we run into issues preventing IDEs to import projects
        // (https://github.com/quarkusio/quarkus/issues/41825) and ./gradlew refreshVersions seems to break as well.
        Configuration conditionalDepConfiguration = project.getConfigurations().detachedConfiguration();
        enforcedPlatforms.getExcludeRules().forEach(rule -> {
            Map<String, String> excludeProperties = new HashMap<>();
            excludeProperties.put("group", rule.getGroup());
            excludeProperties.put("module", rule.getModule());
            conditionalDepConfiguration.exclude(excludeProperties);
        });
        enforcedPlatforms.getAllDependencies().forEach(dependency -> {
            conditionalDepConfiguration.getDependencies().add(dependency);
        });
        conditionalDepConfiguration.getDependencies().add(conditionalDep);
        return conditionalDepConfiguration;
    }

    private void enableConditionalDependency(ModuleVersionIdentifier dependency) {
        final Set<ExtensionDependency<?>> extensions = featureVariants.remove(getFeatureKey(dependency));
        if (extensions == null) {
            return;
        }
        extensions.forEach(e -> e.importConditionalDependency(project.getDependencies(), dependency));
    }

    private boolean exist(List<ArtifactKey> dependencies) {
        return existingArtifacts.containsAll(dependencies);
    }

    private boolean exists(Dependency dependency) {
        return existingArtifacts
                .contains(ArtifactKey.of(dependency.getGroup(), dependency.getName(), null, ArtifactCoords.TYPE_JAR));
    }

    public boolean exists(ExtensionDependency<?> dependency) {
        return existingArtifacts
                .contains(ArtifactKey.of(dependency.getGroup(), dependency.getName(), null, ArtifactCoords.TYPE_JAR));
    }

    private static ArtifactKey getFeatureKey(ModuleVersionIdentifier version) {
        return ArtifactKey.ga(version.getGroup(), version.getName());
    }

    private static ArtifactKey getFeatureKey(Dependency version) {
        return ArtifactKey.ga(version.getGroup(), version.getName());
    }

    private static ArtifactKey getKey(ResolvedArtifact a) {
        return ArtifactKey.of(a.getModuleVersion().getId().getGroup(), a.getName(), a.getClassifier(), a.getType());
    }
}
