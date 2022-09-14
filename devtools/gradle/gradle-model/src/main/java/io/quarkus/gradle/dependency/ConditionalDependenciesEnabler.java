package io.quarkus.gradle.dependency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;

import io.quarkus.gradle.tooling.dependency.DependencyUtils;
import io.quarkus.gradle.tooling.dependency.ExtensionDependency;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.runtime.LaunchMode;

public class ConditionalDependenciesEnabler {

    /**
     * Links dependencies to extensions
     */
    private final Map<GACT, Set<ExtensionDependency>> featureVariants = new HashMap<>();
    /**
     * Despite its name, only contains extensions which have no conditional dependencies, or have
     * resolved their conditional dependencies.
     */
    private final Map<ModuleVersionIdentifier, ExtensionDependency> allExtensions = new HashMap<>();
    private final Project project;
    private final Configuration enforcedPlatforms;
    private final Set<ArtifactKey> existingArtifacts = new HashSet<>();
    private final List<Dependency> unsatisfiedConditionalDeps = new ArrayList<>();

    public ConditionalDependenciesEnabler(Project project, LaunchMode mode, Configuration platforms) {
        this.project = project;
        this.enforcedPlatforms = platforms;

        // Get runtimeClasspath (quarkusProdBaseRuntimeClasspathConfiguration to be exact)
        Configuration baseRuntimeConfig = project.getConfigurations()
                .getByName(ApplicationDeploymentClasspathBuilder.getBaseRuntimeConfigName(mode));

        if (!baseRuntimeConfig.getIncoming().getDependencies().isEmpty()) {
            // Gather all extensions from the full resolved dependency tree
            collectConditionalDependencies(baseRuntimeConfig.getResolvedConfiguration().getResolvedArtifacts());
            // If there are any extensions which had unresolved conditional dependencies:
            while (!unsatisfiedConditionalDeps.isEmpty()) {
                boolean satisfiedConditionalDeps = false;
                final int originalUnsatisfiedCount = unsatisfiedConditionalDeps.size();
                int i = 0;
                // Go through each unsatisfied/unresolved dependency once:
                while (i < unsatisfiedConditionalDeps.size()) {
                    final Dependency conditionalDep = unsatisfiedConditionalDeps.get(i);
                    // Try to resolve it with the latest evolved graph available
                    if (resolveConditionalDependency(conditionalDep)) {
                        // Mark the resolution as a success, so we know the graph has evolved
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

    public Collection<ExtensionDependency> getAllExtensions() {
        return allExtensions.values();
    }

    private void reset() {
        featureVariants.clear();
        existingArtifacts.clear();
        unsatisfiedConditionalDeps.clear();
    }

    private void collectConditionalDependencies(Set<ResolvedArtifact> runtimeArtifacts) {
        addToMasterList(runtimeArtifacts);
        var artifactExtensions = getArtifactExtensions(runtimeArtifacts);
        allExtensions.putAll(artifactExtensions);
        artifactExtensions.forEach((ignored, extension) -> queueAbsentExtensionConditionalDependencies(extension));
    }

    private void addToMasterList(Set<ResolvedArtifact> artifacts) {
        artifacts.stream().map(ConditionalDependenciesEnabler::getKey).forEach(existingArtifacts::add);
    }

    private Map<ModuleVersionIdentifier, ExtensionDependency> getArtifactExtensions(Set<ResolvedArtifact> runtimeArtifacts) {
        return runtimeArtifacts.stream()
                .flatMap(artifact -> DependencyUtils.getOptionalExtensionInfo(project, artifact).stream())
                .collect(Collectors.toMap(ExtensionDependency::getExtensionId, Function.identity()));
    }

    private void queueAbsentExtensionConditionalDependencies(ExtensionDependency extension) {
        extension.getConditionalDependencies().stream().filter(dep -> !exists(dep))
                .forEach(dep -> queueConditionalDependency(extension, dep));
    }

    private boolean resolveConditionalDependency(Dependency conditionalDep) {

        final Configuration conditionalDeps = createConditionalDependenciesConfiguration(project, conditionalDep);
        Set<ResolvedArtifact> resolvedArtifacts = conditionalDeps.getResolvedConfiguration().getResolvedArtifacts();

        boolean isConditionalDependencyResolved = resolvedArtifacts.stream()
                .filter(artifact -> areEquals(conditionalDep, artifact))
                .flatMap(artifact -> DependencyUtils.getOptionalExtensionInfo(project, artifact).stream())
                .filter(extension -> extension.getDependencyConditions().isEmpty()
                        || exist(extension.getDependencyConditions()))
                .findFirst().map(extension -> {
                    enableConditionalDependency(extension.getExtensionId());
                    return true;
                }).orElse(false);

        if (isConditionalDependencyResolved) {
            addToMasterList(resolvedArtifacts);
            var artifactExtensions = getArtifactExtensions(resolvedArtifacts);
            artifactExtensions.forEach((id, extension) -> extension.setConditional(true));
            allExtensions.putAll(artifactExtensions);
            artifactExtensions.forEach((ignored, extension) -> queueAbsentExtensionConditionalDependencies(extension));
        }

        return isConditionalDependencyResolved;
    }

    private boolean areEquals(Dependency dependency, ResolvedArtifact artifact) {
        return dependency.getName().equals(artifact.getName())
                && Objects.equals(dependency.getVersion(), artifact.getModuleVersion().getId().getVersion())
                && artifact.getModuleVersion().getId().getGroup().equals(dependency.getGroup());
    }

    private void queueConditionalDependency(ExtensionDependency extension, Dependency conditionalDep) {
        // 1. Add to master list of unresolved/unsatisfied dependencies
        // 2. Add map entry to link dependency to extension
        featureVariants.computeIfAbsent(getFeatureKey(conditionalDep), k -> {
            unsatisfiedConditionalDeps.add(conditionalDep);
            return new HashSet<>();
        }).add(extension);
    }

    private Configuration createConditionalDependenciesConfiguration(Project project, Dependency conditionalDep) {
        Configuration conditionalDepConfiguration = project.getConfigurations()
                .detachedConfiguration()
                .extendsFrom(enforcedPlatforms);
        conditionalDepConfiguration.getDependencies().add(conditionalDep);
        return conditionalDepConfiguration;
    }

    private void enableConditionalDependency(ModuleVersionIdentifier dependency) {
        final Set<ExtensionDependency> extensions = featureVariants.remove(getFeatureKey(dependency));
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

    public boolean exists(ExtensionDependency dependency) {
        return existingArtifacts
                .contains(ArtifactKey.of(dependency.getGroup(), dependency.getName(), null, ArtifactCoords.TYPE_JAR));
    }

    private static GACT getFeatureKey(ModuleVersionIdentifier version) {
        return new GACT(version.getGroup(), version.getName());
    }

    private static GACT getFeatureKey(Dependency version) {
        return new GACT(version.getGroup(), version.getName());
    }

    private static ArtifactKey getKey(ResolvedArtifact a) {
        return ArtifactKey.of(a.getModuleVersion().getId().getGroup(), a.getName(), a.getClassifier(), a.getType());
    }
}
