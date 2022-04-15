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
import io.quarkus.maven.dependency.GACT;
import io.quarkus.runtime.LaunchMode;

public class ConditionalDependenciesEnabler {

    private final Map<GACT, Set<ExtensionDependency>> featureVariants = new HashMap<>();
    private final Configuration baseRuntimeConfig;
    private final Map<ModuleVersionIdentifier, ExtensionDependency> allExtensions = new HashMap<>();
    private final Project project;
    private final Collection<Dependency> enforcedPlatforms;
    private final Set<ArtifactKey> existingArtifacts = new HashSet<>();
    private final List<Dependency> unsatisfiedConditionalDeps = new ArrayList<>();

    public ConditionalDependenciesEnabler(Project project, LaunchMode mode,
            Collection<org.gradle.api.artifacts.Dependency> platforms) {
        this.project = project;
        this.enforcedPlatforms = platforms;

        baseRuntimeConfig = project.getConfigurations()
                .getByName(ApplicationDeploymentClasspathBuilder.getBaseRuntimeConfigName(mode));

        if (!baseRuntimeConfig.getIncoming().getDependencies().isEmpty()) {
            collectConditionalDependencies(baseRuntimeConfig.getResolvedConfiguration().getResolvedArtifacts());
            while (!unsatisfiedConditionalDeps.isEmpty()) {
                boolean satisfiedConditionalDeps = false;
                final int originalUnsatisfiedCount = unsatisfiedConditionalDeps.size();
                int i = 0;
                while (i < unsatisfiedConditionalDeps.size()) {
                    final Dependency conditionalDep = unsatisfiedConditionalDeps.get(i);
                    if (resolveConditionalDependency(conditionalDep)) {
                        satisfiedConditionalDeps = true;
                        unsatisfiedConditionalDeps.remove(i);
                    } else {
                        ++i;
                    }
                }
                if (!satisfiedConditionalDeps && unsatisfiedConditionalDeps.size() == originalUnsatisfiedCount) {
                    break;
                }
            }
            reset();
        }

    }

    public Configuration getBaseRuntimeConfiguration() {
        return baseRuntimeConfig;
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
        for (ResolvedArtifact artifact : runtimeArtifacts) {
            existingArtifacts.add(getKey(artifact));
            ExtensionDependency extension = DependencyUtils.getExtensionInfoOrNull(project, artifact);
            if (extension != null) {
                allExtensions.put(extension.getExtensionId(), extension);
                for (Dependency conditionalDep : extension.getConditionalDependencies()) {
                    if (!exists(conditionalDep)) {
                        queueConditionalDependency(extension, conditionalDep);
                    }
                }
            }
        }
    }

    private boolean resolveConditionalDependency(Dependency conditionalDep) {

        final Configuration conditionalDeps = createConditionalDependenciesConfiguration(project, conditionalDep);
        Set<ResolvedArtifact> resolvedArtifacts = conditionalDeps.getResolvedConfiguration().getResolvedArtifacts();

        boolean satisfied = false;
        for (ResolvedArtifact artifact : resolvedArtifacts) {
            if (conditionalDep.getName().equals(artifact.getName())
                    && conditionalDep.getVersion().equals(artifact.getModuleVersion().getId().getVersion())
                    && artifact.getModuleVersion().getId().getGroup().equals(conditionalDep.getGroup())) {
                final ExtensionDependency extensionDependency = DependencyUtils.getExtensionInfoOrNull(project, artifact);
                if (extensionDependency != null && (extensionDependency.getDependencyConditions().isEmpty()
                        || exist(extensionDependency.getDependencyConditions()))) {
                    satisfied = true;
                    enableConditionalDependency(extensionDependency.getExtensionId());
                    break;
                }
            }
        }

        if (!satisfied) {
            return false;
        }

        for (ResolvedArtifact artifact : resolvedArtifacts) {
            existingArtifacts.add(getKey(artifact));
            ExtensionDependency extensionDependency = DependencyUtils.getExtensionInfoOrNull(project, artifact);
            if (extensionDependency == null) {
                continue;
            }
            extensionDependency.setConditional(true);
            allExtensions.put(extensionDependency.getExtensionId(), extensionDependency);
            for (Dependency cd : extensionDependency.getConditionalDependencies()) {
                if (!exists(cd)) {
                    queueConditionalDependency(extensionDependency, cd);
                }
            }
        }
        return satisfied;
    }

    private void queueConditionalDependency(ExtensionDependency extension, Dependency conditionalDep) {
        featureVariants.computeIfAbsent(getFeatureKey(conditionalDep), k -> {
            unsatisfiedConditionalDeps.add(conditionalDep);
            return new HashSet<>();
        }).add(extension);
    }

    private Configuration createConditionalDependenciesConfiguration(Project project, Dependency conditionalDep) {
        final List<Dependency> deps = new ArrayList<>(enforcedPlatforms.size() + 1);
        deps.addAll(enforcedPlatforms);
        deps.add(conditionalDep);
        return project.getConfigurations().detachedConfiguration(deps.toArray(new Dependency[0]));
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
                .contains(ArtifactKey.gact(dependency.getGroup(), dependency.getName(), null, ArtifactCoords.TYPE_JAR));
    }

    public boolean exists(ExtensionDependency dependency) {
        return existingArtifacts
                .contains(ArtifactKey.gact(dependency.getGroup(), dependency.getName(), null, ArtifactCoords.TYPE_JAR));
    }

    private static GACT getFeatureKey(ModuleVersionIdentifier version) {
        return new GACT(version.getGroup(), version.getName());
    }

    private static GACT getFeatureKey(Dependency version) {
        return new GACT(version.getGroup(), version.getName());
    }

    private static ArtifactKey getKey(ResolvedArtifact a) {
        return ArtifactKey.gact(a.getModuleVersion().getId().getGroup(), a.getName(), a.getClassifier(), a.getType());
    }
}
