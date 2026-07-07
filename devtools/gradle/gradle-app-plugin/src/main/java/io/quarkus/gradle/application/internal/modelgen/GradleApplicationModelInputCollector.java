package io.quarkus.gradle.application.internal.modelgen;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;

import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.ArtifactSnapshot;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.ClasspathSnapshot;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.ComponentKey;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.ComponentSnapshot;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.LocalOutputKey;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.LocalOutputSnapshot;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.ModuleCoordinates;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.PlatformArtifactSnapshot;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.WorkspaceSnapshot;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.runtime.LaunchMode;

/**
 * Takes execution-time snapshots of Gradle resolution results for the Gradle-free application-model assembler.
 */
final class GradleApplicationModelInputCollector {

    private final Map<ComponentIdentifier, ComponentKey> componentKeys = new HashMap<>();

    ApplicationModelInputs collect(GenerateModelTask task) {
        ClasspathSnapshot applicationClasspath = classpath(task.getAppClasspath());
        ClasspathSnapshot deploymentClasspath = classpath(task.getDeploymentClasspath());
        ClasspathSnapshot compileOnlyClasspath = classpath(task.getCompileOnlyClasspath());
        return new ApplicationModelInputs(
                workspace(task),
                applicationClasspath,
                deploymentClasspath,
                compileOnlyClasspath,
                platformArtifacts(task.getPlatformInfo()),
                localOutputs(task.getLocalClassOutputArtifacts().get(), task.getLocalResourceOutputArtifacts().get()),
                task.getLaunchMode().get() != LaunchMode.NORMAL);
    }

    ApplicationModelInputs collect(Project project, ApplicationModelResolutionViews resolutionViews,
            LaunchMode mode, boolean workspaceDiscovery) {
        ApplicationModelResolutionViews.ModeViews modeViews = resolutionViews.forMode(mode);
        return new ApplicationModelInputs(
                workspace(project, mode),
                classpath(modeViews.runtime()),
                classpath(modeViews.deployment()),
                classpath(modeViews.compileOnly()),
                platformArtifacts(resolutionViews.platformConfiguration()),
                workspaceDiscovery
                        ? localOutputs(modeViews.localOutputs().classArtifacts().get(),
                                modeViews.localOutputs().resourceArtifacts().get(), true)
                        : new LocalOutputSnapshot(Map.of()),
                false);
    }

    ClasspathSnapshot classpath(ResolvedClasspath classpath) {
        Map<ComponentIdentifier, List<ResolvedArtifact>> artifacts = classpath
                .resolvedArtifactsByComponentIdentifier();
        Map<ComponentKey, ComponentSnapshot> components = new LinkedHashMap<>();
        List<ComponentKey> rootDependencies = resolvedDependencies(classpath.getRoot().get().getDependencies(), artifacts,
                components);
        ModuleVersionIdentifier rootModule = Objects.requireNonNull(classpath.getRoot().get().getModuleVersion());
        List<Path> allResolvedFiles = classpath.getAllResolvedFiles().getFiles().stream()
                .map(File::toPath)
                .toList();
        return new ClasspathSnapshot(moduleCoordinates(rootModule), rootDependencies, components, allResolvedFiles);
    }

    private ClasspathSnapshot classpath(Configuration configuration) {
        Map<ComponentIdentifier, List<ResolvedArtifact>> artifacts = new LinkedHashMap<>();
        for (ResolvedArtifactResult artifact : configuration.getIncoming().getArtifacts().getArtifacts()) {
            String type = artifact.getVariant().getAttributes()
                    .getAttribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE);
            ComponentIdentifier component = artifact.getId().getComponentIdentifier();
            artifacts.computeIfAbsent(component, ignored -> new ArrayList<>())
                    .add(new ResolvedArtifact(artifact.getId(), artifact.getFile(), type));
        }
        var root = configuration.getIncoming().getResolutionResult().getRoot();
        Map<ComponentKey, ComponentSnapshot> components = new LinkedHashMap<>();
        List<ComponentKey> rootDependencies = resolvedDependencies(root.getDependencies(), artifacts, components);
        ModuleVersionIdentifier rootModule = Objects.requireNonNull(root.getModuleVersion());
        List<Path> allResolvedFiles = configuration.getIncoming().getArtifacts().getArtifactFiles().getFiles().stream()
                .map(File::toPath)
                .toList();
        return new ClasspathSnapshot(moduleCoordinates(rootModule), rootDependencies, components, allResolvedFiles);
    }

    private List<ComponentKey> resolvedDependencies(
            Set<? extends DependencyResult> dependencies,
            Map<ComponentIdentifier, List<ResolvedArtifact>> artifacts,
            Map<ComponentKey, ComponentSnapshot> components) {
        List<ComponentKey> resolved = new ArrayList<>(dependencies.size());
        for (DependencyResult dependency : dependencies) {
            if (dependency instanceof ResolvedDependencyResult result) {
                ComponentIdentifier componentIdentifier = result.getSelected().getId();
                ComponentKey key = key(componentIdentifier);
                resolved.add(key);
                if (!components.containsKey(key)) {
                    collectComponent(result, artifacts, components);
                }
            }
        }
        return List.copyOf(resolved);
    }

    private void collectComponent(
            ResolvedDependencyResult dependency,
            Map<ComponentIdentifier, List<ResolvedArtifact>> artifacts,
            Map<ComponentKey, ComponentSnapshot> components) {
        ComponentIdentifier componentIdentifier = dependency.getSelected().getId();
        ComponentKey key = key(componentIdentifier);
        ModuleVersionIdentifier moduleVersion = Objects.requireNonNull(dependency.getSelected().getModuleVersion());

        // Insert a placeholder before descending so a cyclic dependency graph terminates.
        components.put(key, new ComponentSnapshot(key, moduleCoordinates(moduleVersion), List.of(), List.of()));
        List<ComponentKey> childDependencies = resolvedDependencies(dependency.getSelected().getDependencies(), artifacts,
                components);
        List<ArtifactSnapshot> componentArtifacts = artifacts.getOrDefault(componentIdentifier, List.of()).stream()
                .map(artifact -> new ArtifactSnapshot(artifact.file.toPath(), artifact.type))
                .toList();
        components.put(key, new ComponentSnapshot(key, moduleCoordinates(moduleVersion), childDependencies,
                componentArtifacts));
    }

    private ComponentKey key(ComponentIdentifier componentIdentifier) {
        return componentKeys.computeIfAbsent(componentIdentifier, ignored -> new ComponentKey(componentKeys.size()));
    }

    private static WorkspaceSnapshot workspace(GenerateModelTask task) {
        return new WorkspaceSnapshot(
                task.getProjectDirectory().get().getAsFile().toPath(),
                task.getBuildDirectory().get().getAsFile().toPath(),
                task.getProjectBuildFile().get().getAsFile().toPath(),
                paths(task.getApplicationClassesDirectories().getFiles()),
                paths(task.getApplicationResourcesDirectories().getFiles()),
                stringPaths(task.getApplicationSourceDirectoryPaths().get()),
                stringPaths(task.getApplicationResourceSourceDirectoryPaths().get()),
                stringPaths(task.getTestClassesDirectoryPaths().get()),
                stringPaths(task.getTestResourcesDirectoryPaths().get()),
                stringPaths(task.getTestSourceDirectoryPaths().get()),
                stringPaths(task.getTestResourceSourceDirectoryPaths().get()));
    }

    private static WorkspaceSnapshot workspace(Project project, LaunchMode mode) {
        JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
        SourceSet main = java.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        SourceSet test = java.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);
        return new WorkspaceSnapshot(
                project.getLayout().getProjectDirectory().getAsFile().toPath(),
                project.getLayout().getBuildDirectory().get().getAsFile().toPath(),
                project.getBuildFile().toPath(),
                paths(main.getOutput().getClassesDirs().getFiles()),
                resourceOutputPaths(main),
                sourceDirectoryPaths(main.getAllJava()),
                sourceDirectoryPaths(main.getResources()),
                mode == LaunchMode.TEST ? paths(test.getOutput().getClassesDirs().getFiles()) : List.of(),
                mode == LaunchMode.TEST ? resourceOutputPaths(test) : List.of(),
                mode == LaunchMode.TEST ? sourceDirectoryPaths(test.getAllJava()) : List.of(),
                mode == LaunchMode.TEST ? sourceDirectoryPaths(test.getResources()) : List.of());
    }

    private static List<Path> resourceOutputPaths(SourceSet sourceSet) {
        File resourcesDirectory = sourceSet.getOutput().getResourcesDir();
        return resourcesDirectory == null ? List.of() : List.of(resourcesDirectory.toPath());
    }

    private static List<Path> sourceDirectoryPaths(SourceDirectorySet sourceDirectories) {
        return sourceDirectories.getSourceDirectories().getFiles().stream()
                .map(File::toPath)
                .toList();
    }

    private static List<Path> paths(Set<File> files) {
        return files.stream().map(File::toPath).toList();
    }

    private static List<Path> stringPaths(List<String> paths) {
        return paths.stream().map(Path::of).toList();
    }

    private static List<PlatformArtifactSnapshot> platformArtifacts(PlatformInfo platformInfo) {
        return platformArtifacts(platformInfo.getResolvedArtifacts().get());
    }

    private static List<PlatformArtifactSnapshot> platformArtifacts(Configuration configuration) {
        return platformArtifacts(configuration.getIncoming().getArtifacts().getArtifacts());
    }

    private static List<PlatformArtifactSnapshot> platformArtifacts(Set<ResolvedArtifactResult> artifacts) {
        List<PlatformArtifactSnapshot> result = new ArrayList<>();
        for (ResolvedArtifactResult artifact : artifacts) {
            ComponentIdentifier componentIdentifier = artifact.getId().getComponentIdentifier();
            if (!(componentIdentifier instanceof ModuleComponentIdentifier module)) {
                throw new IllegalStateException("Expected an external module platform artifact but got "
                        + componentIdentifier.getDisplayName());
            }
            result.add(new PlatformArtifactSnapshot(module.getGroup(), module.getModule(), module.getVersion(),
                    artifact.getFile().toPath()));
        }
        return List.copyOf(result);
    }

    private LocalOutputSnapshot localOutputs(Set<ResolvedArtifactResult> classOutputs,
            Set<ResolvedArtifactResult> resourceOutputs) {
        return localOutputs(classOutputs, resourceOutputs, false);
    }

    private LocalOutputSnapshot localOutputs(Set<ResolvedArtifactResult> classOutputs,
            Set<ResolvedArtifactResult> resourceOutputs, boolean includeUnmaterializedOutputs) {
        // Task-produced models retain their historical materialized-output semantics. A synchronous Tooling API
        // request must instead describe the producer-declared output contract even before prerequisite tasks run.
        Map<LocalOutputKey, List<Path>> pathsByVariant = new LinkedHashMap<>();
        collectLocalOutputs(classOutputs, pathsByVariant, includeUnmaterializedOutputs);
        collectLocalOutputs(resourceOutputs, pathsByVariant, includeUnmaterializedOutputs);
        return new LocalOutputSnapshot(pathsByVariant);
    }

    private void collectLocalOutputs(Set<ResolvedArtifactResult> artifacts,
            Map<LocalOutputKey, List<Path>> pathsByVariant, boolean includeUnmaterializedOutputs) {
        for (ResolvedArtifactResult artifact : artifacts) {
            ComponentIdentifier componentIdentifier = artifact.getId().getComponentIdentifier();
            if (componentIdentifier instanceof ProjectComponentIdentifier
                    && (includeUnmaterializedOutputs || artifact.getFile().exists())) {
                LocalOutputKey key = new LocalOutputKey(key(componentIdentifier), classifierFromOutputPath(artifact));
                List<Path> paths = pathsByVariant.computeIfAbsent(key, ignored -> new ArrayList<>());
                Path path = artifact.getFile().toPath();
                if (!paths.contains(path)) {
                    paths.add(path);
                }
            }
        }
    }

    private static String classifierFromOutputPath(ResolvedArtifactResult artifact) {
        String sourceSetName = artifact.getFile().getName();
        if (SourceSet.MAIN_SOURCE_SET_NAME.equals(sourceSetName)) {
            return ArtifactCoords.DEFAULT_CLASSIFIER;
        }
        StringBuilder result = new StringBuilder(sourceSetName.length());
        for (int i = 0; i < sourceSetName.length(); i++) {
            char character = sourceSetName.charAt(i);
            if (Character.isUpperCase(character)) {
                if (i > 0) {
                    result.append('-');
                }
                result.append(Character.toLowerCase(character));
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }

    private static ModuleCoordinates moduleCoordinates(ModuleVersionIdentifier module) {
        return new ModuleCoordinates(module.getGroup(), module.getName(), module.getVersion());
    }
}
