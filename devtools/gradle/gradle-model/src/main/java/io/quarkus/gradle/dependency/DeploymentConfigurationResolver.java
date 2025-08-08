package io.quarkus.gradle.dependency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.internal.tasks.TaskDependencyFactory;
import org.gradle.api.provider.ListProperty;

import io.quarkus.gradle.tooling.dependency.DependencyUtils;
import io.quarkus.gradle.tooling.dependency.ExtensionDependency;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.runtime.LaunchMode;

/**
 * Registers and initializes project's build time (deployment) classpath configuration
 */
public class DeploymentConfigurationResolver {

    private static final byte COLLECT_TOP_EXTENSIONS = 0b001;

    /**
     * Registers and initializes project's build time (deployment) classpath configuration for a given project
     * and launch mode.
     * <p/>
     * The configuration will re-use component variants added previously by the {@link QuarkusComponentVariants}.
     * <p/>
     * Deployment dependencies that could not be added using variants, will be added as direct dependencies
     * of the configuration.
     *
     * @param project project
     * @param mode launch mode
     * @param configurationName configuration name
     * @param taskDependencyFactory task dependency factory
     */
    public static void registerDeploymentConfiguration(Project project, LaunchMode mode, String configurationName,
            TaskDependencyFactory taskDependencyFactory) {
        project.getConfigurations().register(configurationName,
                config -> new DeploymentConfigurationResolver(project, config, mode, taskDependencyFactory));
    }

    private final Project project;
    private final TaskDependencyFactory taskDependencyFactory;
    private byte walkingFlags;

    private DeploymentConfigurationResolver(Project project, Configuration deploymentConfig, LaunchMode mode,
            TaskDependencyFactory taskDependencyFactory) {
        this.project = project;
        this.taskDependencyFactory = taskDependencyFactory;

        final Configuration baseRuntimeConfig = project.getConfigurations()
                .getByName(ApplicationDeploymentClasspathBuilder.getFinalRuntimeConfigName(mode));
        deploymentConfig.setCanBeConsumed(false);
        deploymentConfig.extendsFrom(baseRuntimeConfig);
        deploymentConfig.shouldResolveConsistentlyWith(baseRuntimeConfig);

        ListProperty<Dependency> dependencyListProperty = project.getObjects().listProperty(Dependency.class);
        final AtomicReference<Collection<Dependency>> directDeploymentDeps = new AtomicReference<>();
        // the following provider appears to be called 3 times for some reason,
        // this is the reason for this atomic reference checks
        deploymentConfig.getDependencies().addAllLater(dependencyListProperty.value(project.provider(() -> {
            Collection<Dependency> directDeps = directDeploymentDeps.get();
            if (directDeps == null) {
                if (!baseRuntimeConfig.getIncoming().getDependencies().isEmpty()) {
                    directDeps = collectDirectDeploymentDeps(baseRuntimeConfig.getResolvedConfiguration());
                } else {
                    directDeps = List.of();
                }
                directDeploymentDeps.set(directDeps);
            }
            return directDeps;
        })));
        QuarkusComponentVariants.setDeploymentAndConditionalAttributes(deploymentConfig, project, mode);
    }

    private Collection<Dependency> collectDirectDeploymentDeps(ResolvedConfiguration baseConfig) {
        return collectDirectDeploymentDeps(processRuntimeDeps(baseConfig));
    }

    private Map<ArtifactKey, ProcessedDependency> processRuntimeDeps(ResolvedConfiguration baseConfig) {
        final Map<ArtifactKey, ProcessedDependency> allDeps = new HashMap<>();
        setWalkingFlags(COLLECT_TOP_EXTENSIONS);
        for (var dep : baseConfig.getFirstLevelModuleDependencies()) {
            processDependency(null, dep, allDeps);
        }
        walkingFlags = 0;
        return allDeps;
    }

    private void processDependency(ProcessedDependency parent,
            ResolvedDependency dep,
            Map<ArtifactKey, ProcessedDependency> allDeps) {
        boolean processChildren = false;
        int depFlags = 0;
        var artifacts = dep.getModuleArtifacts();
        ProcessedDependency processedDep = null;
        if (artifacts.isEmpty()) {
            processChildren = true;
        } else {
            for (var artifact : artifacts) {
                processedDep = allDeps.computeIfAbsent(DependencyUtils.getKey(artifact), key -> {
                    final ProcessedDependency pd = new ProcessedDependency(parent, dep,
                            artifact.getId().getComponentIdentifier() instanceof ProjectComponentIdentifier);
                    if (isWalkingFlagsOn(COLLECT_TOP_EXTENSIONS)) {
                        pd.ext = DependencyUtils.getExtensionInfoOrNull(project, artifact);
                        if (pd.ext != null) {
                            pd.setFlags(DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
                        }
                    }
                    return pd;
                });
                if (processedDep.setFlags(DependencyFlags.VISITED)) {
                    processChildren = true;
                    depFlags |= processedDep.flags;
                }
            }
        }
        if (processChildren) {
            boolean stopCollectingTopExt = isWalkingFlagsOn(COLLECT_TOP_EXTENSIONS)
                    && (depFlags & DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT) > 0;
            if (stopCollectingTopExt) {
                clearWalkingFlags(COLLECT_TOP_EXTENSIONS);
            }
            for (var child : dep.getChildren()) {
                processDependency(processedDep, child, allDeps);
            }
            if (stopCollectingTopExt) {
                setWalkingFlags(COLLECT_TOP_EXTENSIONS);
            }
        }
    }

    private Collection<Dependency> collectDirectDeploymentDeps(Map<ArtifactKey, ProcessedDependency> allRuntimeDeps) {
        final List<Dependency> directDeploymentDeps = new ArrayList<>();
        for (var processedDep : allRuntimeDeps.values()) {
            if (processedDep.ext != null &&
                    processedDep.hasLocalParent() &&
                    // if it's an extension and its deployment artifact is not a runtime dependency (e.g. deployment tests)
                    !allRuntimeDeps.containsKey(
                            ArtifactKey.of(processedDep.ext.getDeploymentGroup(), processedDep.ext.getDeploymentName(),
                                    ArtifactCoords.DEFAULT_CLASSIFIER, ArtifactCoords.TYPE_JAR))) {
                directDeploymentDeps
                        .add(DependencyUtils.createDeploymentDependency(project.getDependencies(), processedDep.ext));
            }
        }
        return directDeploymentDeps;
    }

    private boolean isWalkingFlagsOn(byte flags) {
        return (walkingFlags & flags) == flags;
    }

    private void clearWalkingFlags(byte flags) {
        walkingFlags &= (byte) (walkingFlags ^ flags);
    }

    private boolean setWalkingFlags(byte flags) {
        return walkingFlags != (walkingFlags |= flags);
    }

    private static class ProcessedDependency {

        final ProcessedDependency parent;
        final ResolvedDependency dep;
        final boolean local;
        int flags;
        ExtensionDependency<?> ext;

        private ProcessedDependency(ProcessedDependency parent, ResolvedDependency dep, boolean local) {
            this.parent = parent;
            this.dep = dep;
            this.local = local;
        }

        private boolean setFlags(int flags) {
            return this.flags != (this.flags |= flags);
        }

        private boolean hasLocalParent() {
            return parent == null || parent.local;
        }
    }
}
