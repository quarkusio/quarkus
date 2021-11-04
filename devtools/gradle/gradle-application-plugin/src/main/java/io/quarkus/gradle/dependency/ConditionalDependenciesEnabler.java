package io.quarkus.gradle.dependency;

import java.util.ArrayList;
import java.util.Collection;
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

import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.maven.dependency.GACT;

public class ConditionalDependenciesEnabler {

    private final Map<GACT, Set<ExtensionDependency>> featureVariants = new HashMap<>();
    private final Map<ModuleVersionIdentifier, ExtensionDependency> allExtensions = new HashMap<>();
    private final Project project;
    private final Configuration baseConfiguration;
    private final Set<ResolvedArtifact> allRuntimeArtifacts = new HashSet<>();
    private final List<Dependency> unsatisfiedConditionalDeps = new ArrayList<>();
    private List<Dependency> enforcedPlatforms;

    public ConditionalDependenciesEnabler(Project project, String baseConfigurationName) {
        this.project = project;
        this.baseConfiguration = project.getConfigurations().getByName(baseConfigurationName);
    }

    public Collection<ExtensionDependency> declareConditionalDependencies() {
        if (baseConfiguration.getIncoming().getDependencies().isEmpty()) {
            return Collections.emptySet();
        }

        final Configuration resolvedConfiguration = DependencyUtils.duplicateConfiguration(project, baseConfiguration);
        enforcedPlatforms = ToolingUtils.getEnforcedPlatforms(baseConfiguration);

        collectConditionalDependencies(resolvedConfiguration.getResolvedConfiguration().getResolvedArtifacts());
        if (unsatisfiedConditionalDeps.isEmpty()) {
            return allExtensions.values();
        }

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

        final List<ExtensionDependency> result = new ArrayList<>(allExtensions.values());
        reset();
        return result;
    }

    private void reset() {
        featureVariants.clear();
        allExtensions.clear();
        allRuntimeArtifacts.clear();
        unsatisfiedConditionalDeps.clear();
    }

    private void collectConditionalDependencies(Set<ResolvedArtifact> runtimeArtifacts) {
        for (ResolvedArtifact artifact : runtimeArtifacts) {
            allRuntimeArtifacts.add(artifact);
            ExtensionDependency extension = DependencyUtils.getExtensionInfoOrNull(project, artifact);
            if (extension != null) {
                allExtensions.put(extension.extensionId, extension);
                for (Dependency conditionalDep : extension.conditionalDependencies) {
                    if (!DependencyUtils.exists(allRuntimeArtifacts, conditionalDep)) {
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
                if (extensionDependency != null && (extensionDependency.dependencyConditions.isEmpty()
                        || DependencyUtils.exist(allRuntimeArtifacts, extensionDependency.dependencyConditions))) {
                    satisfied = true;
                    enableConditionalDependency(extensionDependency.extensionId);
                    break;
                }
            }
        }

        if (!satisfied) {
            return false;
        }

        for (ResolvedArtifact artifact : resolvedArtifacts) {
            allRuntimeArtifacts.add(artifact);
            ExtensionDependency extensionDependency = DependencyUtils.getExtensionInfoOrNull(project, artifact);
            if (extensionDependency == null) {
                continue;
            }
            extensionDependency.setConditional(true);
            allExtensions.put(extensionDependency.extensionId, extensionDependency);
            for (Dependency cd : extensionDependency.conditionalDependencies) {
                if (!DependencyUtils.exists(allRuntimeArtifacts, cd)) {
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
        final Dependency[] deps = new Dependency[enforcedPlatforms.size() + 1];
        for (int i = 0; i < enforcedPlatforms.size(); ++i) {
            deps[i] = enforcedPlatforms.get(i);
        }
        deps[deps.length - 1] = conditionalDep;
        return project.getConfigurations().detachedConfiguration(deps);
    }

    private void enableConditionalDependency(ModuleVersionIdentifier dependency) {
        final Set<ExtensionDependency> extensions = featureVariants.remove(getFeatureKey(dependency));
        if (extensions == null) {
            return;
        }
        extensions.forEach(e -> e.importConditionalDependency(project.getDependencies(), dependency));
    }

    private static GACT getFeatureKey(ModuleVersionIdentifier version) {
        return new GACT(version.getGroup(), version.getName());
    }

    private static GACT getFeatureKey(Dependency version) {
        return new GACT(version.getGroup(), version.getName());
    }
}
