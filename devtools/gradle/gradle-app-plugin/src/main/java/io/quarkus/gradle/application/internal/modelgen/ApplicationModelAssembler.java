package io.quarkus.gradle.application.internal.modelgen;

import static io.quarkus.gradle.model.pom.ApplicationModelBuilderSupport.addFileDependencies;
import static io.quarkus.gradle.model.pom.ApplicationModelBuilderSupport.clearFlag;
import static io.quarkus.gradle.model.pom.ApplicationModelBuilderSupport.isFlagOn;
import static io.quarkus.gradle.model.pom.ApplicationModelBuilderSupport.processQuarkusDependency;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.PlatformImportsImpl;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.DefaultArtifactSources;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.ArtifactSnapshot;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.ClasspathSnapshot;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.ComponentKey;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.ComponentSnapshot;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.LocalOutputSnapshot;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.ModuleCoordinates;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.PlatformArtifactSnapshot;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.WorkspaceSnapshot;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathList;

/**
 * Builds a Quarkus application model from immutable inputs that do not retain Gradle model objects.
 */
final class ApplicationModelAssembler {

    /* @formatter:off */
    private static final byte COLLECT_TOP_EXTENSION_RUNTIME_NODES = 0b001;
    private static final byte COLLECT_DIRECT_DEPS =                 0b010;
    private static final byte COLLECT_RELOADABLE_MODULES =          0b100;
    /* @formatter:on */

    ApplicationModelBuilder assemble(ApplicationModelInputs inputs) {
        WorkspaceModule.Mutable workspaceModule = workspaceModule(inputs);
        ResolvedDependencyBuilder appArtifact = applicationArtifact(inputs, workspaceModule);
        ApplicationModelBuilder modelBuilder = new ApplicationModelBuilder()
                .setAppArtifact(appArtifact)
                .setPlatformImports(platformImports(inputs.platformArtifacts()))
                .addReloadableWorkspaceModule(appArtifact.getKey());

        collectDependencies(inputs.applicationClasspath(), modelBuilder, workspaceModule, inputs.localOutputs(),
                inputs.reloadableWorkspaceDependencies());
        collectExtensionDependencies(inputs.deploymentClasspath(), modelBuilder);
        collectCompileOnlyDependencies(inputs.compileOnlyClasspath(), modelBuilder);
        return modelBuilder;
    }

    private static WorkspaceModule.Mutable workspaceModule(ApplicationModelInputs inputs) {
        ModuleCoordinates coordinates = inputs.applicationClasspath().rootCoordinates();
        WorkspaceSnapshot workspace = inputs.workspace();
        WorkspaceModule.Mutable module = WorkspaceModule.builder()
                .setModuleId(WorkspaceModuleId.of(coordinates.group(), coordinates.name(), coordinates.version()))
                .setModuleDir(workspace.moduleDirectory())
                .setBuildDir(workspace.buildDirectory())
                .setBuildFile(workspace.buildFile());
        Path sourceOutputDir = firstPath(workspace.applicationClassesDirectories());
        Path resourceOutputDir = firstPath(workspace.applicationResourcesDirectories());
        if (sourceOutputDir != null || resourceOutputDir != null) {
            module.addArtifactSources(new DefaultArtifactSources(
                    ArtifactSources.MAIN,
                    sourceOutputDir == null ? List.of()
                            : sourceDirs(workspace.applicationSourceDirectories(), sourceOutputDir),
                    resourceOutputDir == null ? List.of()
                            : sourceDirs(workspace.applicationResourceSourceDirectories(), resourceOutputDir)));
        }
        if (!workspace.testClassesDirectories().isEmpty() || !workspace.testResourcesDirectories().isEmpty()) {
            module.addArtifactSources(new DefaultArtifactSources(
                    ArtifactSources.TEST,
                    sourceDirs(workspace.testSourceDirectories(), workspace.testClassesDirectories()),
                    sourceDirs(workspace.testResourceSourceDirectories(), workspace.testResourcesDirectories())));
        }
        return module;
    }

    private static List<SourceDir> sourceDirs(List<Path> directories, Path outputDirectory) {
        return directories.stream()
                .sorted()
                .map(sourceDirectory -> SourceDir.of(sourceDirectory, outputDirectory))
                .toList();
    }

    private static List<SourceDir> sourceDirs(List<Path> directories, List<Path> outputDirectories) {
        return directories.stream()
                .sorted()
                .flatMap(sourceDirectory -> outputDirectories.stream().sorted()
                        .map(outputDirectory -> SourceDir.of(sourceDirectory, outputDirectory)))
                .toList();
    }

    private static Path firstPath(List<Path> directories) {
        return directories.stream().sorted().findFirst().orElse(null);
    }

    private static ResolvedDependencyBuilder applicationArtifact(ApplicationModelInputs inputs,
            WorkspaceModule.Mutable workspaceModule) {
        ModuleCoordinates coordinates = inputs.applicationClasspath().rootCoordinates();
        ResolvedDependencyBuilder appArtifact = ResolvedDependencyBuilder.newInstance()
                .setGroupId(coordinates.group())
                .setArtifactId(coordinates.name())
                .setVersion(coordinates.version());

        PathList.Builder paths = PathList.builder();
        collectExistingDirectories(inputs.workspace().applicationClassesDirectories(), paths);
        collectExistingDirectories(inputs.workspace().applicationResourcesDirectories(), paths);
        PathList resolvedPaths = paths.build();
        if (resolvedPaths.isEmpty()) {
            appArtifact.setResolvedPaths(PathList.empty());
        } else {
            appArtifact.setResolvedPaths(resolvedPaths);
            appArtifact.setReloadable().setWorkspaceModule();
        }
        return appArtifact.setWorkspaceModule(workspaceModule);
    }

    private static void collectExistingDirectories(List<Path> directories, PathList.Builder paths) {
        for (Path directory : directories) {
            if (Files.exists(directory)) {
                paths.add(directory);
            }
        }
    }

    private static PlatformImportsImpl platformImports(List<PlatformArtifactSnapshot> artifacts) {
        PlatformImportsImpl result = new PlatformImportsImpl();
        for (PlatformArtifactSnapshot artifact : artifacts) {
            String fileName = artifact.path().getFileName().toString();
            if (fileName.endsWith(".json")) {
                result.addPlatformDescriptor(artifact.group(), artifact.name(), artifact.version(),
                        PlatformInfo.PLATFORM_DESCRIPTOR_ARTIFACT_TYPE, artifact.version());
            } else if (fileName.endsWith(".properties")) {
                try {
                    result.addPlatformProperties(artifact.group(), artifact.name(), ArtifactCoords.DEFAULT_CLASSIFIER,
                            PlatformInfo.PLATFORM_PROPERTIES_ARTIFACT_TYPE, artifact.version(), artifact.path());
                } catch (AppModelResolverException e) {
                    throw new IllegalStateException("Failed to add platform properties " + artifact.path(), e);
                }
            }
        }
        return result;
    }

    private static void collectDependencies(ClasspathSnapshot classpath,
            ApplicationModelBuilder modelBuilder,
            WorkspaceModule.Mutable workspaceModule,
            LocalOutputSnapshot localOutputs,
            boolean reloadableWorkspaceDependencies) {
        Set<Path> collectedArtifactFiles = new HashSet<>(classpath.components().size());
        Set<ComponentKey> processedComponents = new HashSet<>();
        Map<ComponentKey, WorkspaceModule.Mutable> localWorkspaceModules = new HashMap<>();
        Set<ComponentKey> rootDependencies = Set.copyOf(classpath.rootDependencies());
        byte rootFlags = (byte) (COLLECT_TOP_EXTENSION_RUNTIME_NODES | COLLECT_DIRECT_DEPS);
        if (reloadableWorkspaceDependencies) {
            rootFlags |= COLLECT_RELOADABLE_MODULES;
        }
        for (ComponentKey rootDependency : classpath.rootDependencies()) {
            collectDependencies(rootDependency, classpath, modelBuilder, workspaceModule, collectedArtifactFiles,
                    processedComponents, rootFlags, localOutputs, localWorkspaceModules,
                    rootDependencies, workspaceModule, rootFlags);
        }

        Set<File> fileDependencies = new HashSet<>(classpath.allResolvedFiles().size());
        for (Path file : classpath.allResolvedFiles()) {
            fileDependencies.add(file.toFile());
        }
        for (Path file : collectedArtifactFiles) {
            fileDependencies.remove(file.toFile());
        }
        addFileDependencies(modelBuilder, fileDependencies);
    }

    private static void collectDependencies(
            ComponentKey componentKey,
            ClasspathSnapshot classpath,
            ApplicationModelBuilder modelBuilder,
            WorkspaceModule.Mutable parentModule,
            Set<Path> collectedArtifactFiles,
            Set<ComponentKey> processedComponents,
            byte flags,
            LocalOutputSnapshot localOutputs,
            Map<ComponentKey, WorkspaceModule.Mutable> localWorkspaceModules,
            Set<ComponentKey> rootDependencies,
            WorkspaceModule.Mutable applicationModule,
            byte rootFlags) {
        boolean rootDependency = rootDependencies.contains(componentKey);
        if (rootDependency) {
            flags |= rootFlags;
        }
        ComponentSnapshot component = classpath.component(componentKey);
        ModuleCoordinates module = component.coordinates();
        WorkspaceModule.Mutable projectModule = localOutputs.hasComponent(componentKey)
                ? localWorkspaceModules.computeIfAbsent(componentKey, ignored -> WorkspaceModule.builder()
                        .setModuleId(WorkspaceModuleId.of(module.group(), module.name(), module.version())))
                : null;
        attachWorkspaceDependencyEdges(component, module, modelBuilder, parentModule, rootDependency, applicationModule);
        if (!processedComponents.add(componentKey)) {
            return;
        }
        if (component.artifacts().isEmpty()) {
            for (ComponentKey dependency : component.dependencies()) {
                collectDependencies(dependency, classpath, modelBuilder, projectModule, collectedArtifactFiles,
                        processedComponents, flags, localOutputs, localWorkspaceModules,
                        rootDependencies, applicationModule, rootFlags);
            }
            return;
        }

        byte childFlags = flags;
        for (ArtifactSnapshot artifact : component.artifacts()) {
            collectedArtifactFiles.add(artifact.path());
            ArtifactKey artifactKey = artifactKey(module, artifact);
            if (!isDependency(artifact)
                    || modelBuilder.getDependency(artifactKey) != null
                    || isApplicationRoot(modelBuilder, artifactKey)) {
                continue;
            }

            ArtifactCoords dependencyCoordinates = new GACTV(artifactKey, module.version());
            ResolvedDependencyBuilder dependencyBuilder = ResolvedDependencyBuilder.newInstance()
                    .setCoords(dependencyCoordinates)
                    .setRuntimeCp()
                    .setDeploymentCp()
                    .setResolvedPaths(localResolvedPathsOrArtifactPath(localOutputs, componentKey, artifact, artifactKey))
                    .setWorkspaceModule(projectModule);
            if (isFlagOn(flags, COLLECT_DIRECT_DEPS)) {
                dependencyBuilder.setDirect(true);
                childFlags = clearFlag(childFlags, COLLECT_DIRECT_DEPS);
            }
            if (processQuarkusDependency(dependencyBuilder, modelBuilder)
                    && isFlagOn(flags, COLLECT_TOP_EXTENSION_RUNTIME_NODES)) {
                dependencyBuilder.setFlags(DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
                childFlags = clearFlag(childFlags, COLLECT_TOP_EXTENSION_RUNTIME_NODES);
            }
            if (isFlagOn(flags, COLLECT_RELOADABLE_MODULES)) {
                if (!dependencyBuilder.isRuntimeExtensionArtifact() && projectModule != null) {
                    dependencyBuilder.setReloadable();
                    modelBuilder.addReloadableWorkspaceModule(artifactKey);
                } else {
                    childFlags = clearFlag(childFlags, COLLECT_RELOADABLE_MODULES);
                }
            }
            modelBuilder.addDependency(dependencyBuilder);
        }

        for (ComponentKey dependency : component.dependencies()) {
            collectDependencies(dependency, classpath, modelBuilder, projectModule, collectedArtifactFiles,
                    processedComponents, childFlags, localOutputs, localWorkspaceModules,
                    rootDependencies, applicationModule, rootFlags);
        }
    }

    private static void attachWorkspaceDependencyEdges(ComponentSnapshot component, ModuleCoordinates module,
            ApplicationModelBuilder modelBuilder, WorkspaceModule.Mutable parentModule, boolean rootDependency,
            WorkspaceModule.Mutable applicationModule) {
        for (ArtifactSnapshot artifact : component.artifacts()) {
            ArtifactKey artifactKey = artifactKey(module, artifact);
            if (!isDependency(artifact) || isApplicationRoot(modelBuilder, artifactKey)) {
                continue;
            }
            ArtifactDependency dependency = new ArtifactDependency(new GACTV(artifactKey, module.version()));
            if (parentModule != null) {
                addWorkspaceDependency(parentModule, dependency);
            }
            if (rootDependency && parentModule != applicationModule) {
                addWorkspaceDependency(applicationModule, dependency);
            }
        }
    }

    private static void addWorkspaceDependency(WorkspaceModule.Mutable module, ArtifactDependency dependency) {
        if (module.getDirectDependencies().stream()
                .noneMatch(existing -> existing.toGACTVString().equals(dependency.toGACTVString()))) {
            module.addDependency(dependency);
        }
    }

    private static PathList localResolvedPathsOrArtifactPath(LocalOutputSnapshot localOutputs,
            ComponentKey component, ArtifactSnapshot artifact, ArtifactKey artifactKey) {
        List<Path> localPaths = localOutputs.get(component, artifactKey.getClassifier());
        return localPaths == null ? PathList.of(artifact.path()) : PathList.from(localPaths);
    }

    private static boolean isApplicationRoot(ApplicationModelBuilder modelBuilder, ArtifactKey artifactKey) {
        return modelBuilder.getApplicationArtifact().getKey().equals(artifactKey);
    }

    private static ArtifactKey artifactKey(ModuleCoordinates module, ArtifactSnapshot artifact) {
        return ArtifactKey.of(module.group(), module.name(),
                resolveClassifier(module.name(), module.version(), artifact.path()), artifact.type());
    }

    private static String resolveClassifier(String artifactId, String version, Path file) {
        String artifactIdVersion = version.isEmpty() || "unspecified".equals(version)
                ? artifactId
                : artifactId + "-" + version;
        String fileName = file.getFileName().toString();
        if ((fileName.endsWith(".jar") || fileName.endsWith(".pom") || fileName.endsWith(".exe"))
                && fileName.startsWith(artifactIdVersion + "-")) {
            return fileName.substring(artifactIdVersion.length() + 1, fileName.length() - 4);
        }
        return ArtifactCoords.DEFAULT_CLASSIFIER;
    }

    private static boolean isDependency(ArtifactSnapshot artifact) {
        String fileName = artifact.path().getFileName().toString();
        return fileName.endsWith(ArtifactCoords.TYPE_JAR)
                || fileName.endsWith(".exe")
                || Files.isDirectory(artifact.path());
    }

    private static void collectExtensionDependencies(ClasspathSnapshot classpath,
            ApplicationModelBuilder modelBuilder) {
        Set<ComponentKey> processedComponents = new HashSet<>();
        for (ComponentKey rootDependency : classpath.rootDependencies()) {
            collectExtensionDependencies(rootDependency, classpath, modelBuilder, processedComponents, false);
        }
    }

    private static void collectExtensionDependencies(
            ComponentKey componentKey,
            ClasspathSnapshot classpath,
            ApplicationModelBuilder modelBuilder,
            Set<ComponentKey> processedComponents,
            boolean clearReloadableFlag) {
        if (!processedComponents.add(componentKey)) {
            if (clearReloadableFlag) {
                clearReloadableWorkspaceModule(classpath.component(componentKey), modelBuilder);
            }
            return;
        }
        ComponentSnapshot component = classpath.component(componentKey);

        // Runtime-to-deployment handoff variants intentionally carry no artifact of their own. Their child
        // dependency is the deployment module that must still be added to the application model.
        boolean clearReloadableFlagChildren = clearReloadableFlag;
        for (ArtifactSnapshot artifact : component.artifacts()) {
            ArtifactKey artifactKey = artifactKey(component.coordinates(), artifact);
            if (!isDependency(artifact) || isApplicationRoot(modelBuilder, artifactKey)) {
                continue;
            }

            ResolvedDependencyBuilder dependency = modelBuilder.getDependency(artifactKey);
            if (dependency == null) {
                ArtifactCoords artifactCoordinates = new GACTV(artifactKey, component.coordinates().version());
                dependency = toDependency(artifactCoordinates, artifact.path());
                modelBuilder.addDependency(dependency);
            }
            dependency.setDeploymentCp();
            if (clearReloadableFlag) {
                clearReloadableWorkspaceModule(modelBuilder, dependency);
            } else if (!dependency.isReloadable()) {
                clearReloadableFlagChildren = true;
            }
        }

        for (ComponentKey dependency : component.dependencies()) {
            collectExtensionDependencies(dependency, classpath, modelBuilder, processedComponents,
                    clearReloadableFlagChildren);
        }
    }

    private static void clearReloadableWorkspaceModule(
            ComponentSnapshot component,
            ApplicationModelBuilder modelBuilder) {
        for (ArtifactSnapshot artifact : component.artifacts()) {
            ResolvedDependencyBuilder dependency = modelBuilder.getDependency(artifactKey(component.coordinates(), artifact));
            if (dependency != null) {
                clearReloadableWorkspaceModule(modelBuilder, dependency);
            }
        }
    }

    private static void clearReloadableWorkspaceModule(ApplicationModelBuilder modelBuilder,
            ResolvedDependencyBuilder dependency) {
        dependency.clearFlag(DependencyFlags.RELOADABLE);
        modelBuilder.removeReloadableWorkspaceModule(dependency.getKey());
    }

    private static void collectCompileOnlyDependencies(ClasspathSnapshot classpath,
            ApplicationModelBuilder modelBuilder) {
        Set<ComponentKey> processedComponents = new HashSet<>();
        for (ComponentKey rootDependency : classpath.rootDependencies()) {
            collectCompileOnlyDependencies(rootDependency, classpath, modelBuilder, processedComponents);
        }
    }

    private static void collectCompileOnlyDependencies(
            ComponentKey componentKey,
            ClasspathSnapshot classpath,
            ApplicationModelBuilder modelBuilder,
            Set<ComponentKey> processedComponents) {
        if (!processedComponents.add(componentKey)) {
            return;
        }
        ComponentSnapshot component = classpath.component(componentKey);
        if (component.artifacts().isEmpty()) {
            return;
        }

        boolean skip = true;
        for (ArtifactSnapshot artifact : component.artifacts()) {
            if (!isDependency(artifact)) {
                continue;
            }
            ArtifactKey artifactKey = artifactKey(component.coordinates(), artifact);
            if (isApplicationRoot(modelBuilder, artifactKey)) {
                continue;
            }

            ResolvedDependencyBuilder dependency = modelBuilder.getDependency(artifactKey);
            if (dependency == null) {
                ArtifactCoords artifactCoordinates = new GACTV(artifactKey, component.coordinates().version());
                dependency = toDependency(artifactCoordinates, artifact.path());
                modelBuilder.addDependency(dependency);
            }
            if (!dependency.isFlagSet(DependencyFlags.COMPILE_ONLY)) {
                skip = false;
                dependency.setFlags(DependencyFlags.COMPILE_ONLY);
            }
        }

        if (!skip) {
            for (ComponentKey dependency : component.dependencies()) {
                collectCompileOnlyDependencies(dependency, classpath, modelBuilder, processedComponents);
            }
        }
    }

    private static ResolvedDependencyBuilder toDependency(ArtifactCoords artifactCoordinates, Path path, int... flags) {
        int allFlags = 0;
        for (int flag : flags) {
            allFlags |= flag;
        }
        return ResolvedDependencyBuilder.newInstance()
                .setCoords(artifactCoordinates)
                .setResolvedPaths(PathList.of(path))
                .setFlags(allFlags);
    }
}
