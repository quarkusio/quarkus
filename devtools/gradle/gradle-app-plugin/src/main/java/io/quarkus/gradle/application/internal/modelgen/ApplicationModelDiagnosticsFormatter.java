package io.quarkus.gradle.application.internal.modelgen;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;

public final class ApplicationModelDiagnosticsFormatter {

    private ApplicationModelDiagnosticsFormatter() {
    }

    public static String format(String modelName, ApplicationModel model, Path projectDirectory, Path buildRoot) {
        StringBuilder diagnostics = new StringBuilder();
        diagnostics.append("Quarkus application model '").append(modelName).append("':");

        diagnostics.append("\nApplication artifact:");
        appendResolvedDependency(diagnostics, model.getAppArtifact(), projectDirectory, buildRoot, "    ");

        diagnostics.append("\nDependencies:");
        StreamSupport.stream(model.getDependenciesWithAnyFlag(
                DependencyFlags.RUNTIME_CP | DependencyFlags.DEPLOYMENT_CP | DependencyFlags.COMPILE_ONLY).spliterator(), false)
                .sorted(Comparator.comparing(ArtifactCoords::toGACTVString))
                .forEach(dependency -> appendResolvedDependency(
                        diagnostics, dependency, projectDirectory, buildRoot, "    "));

        diagnostics.append("\nWorkspace modules:");
        Map<String, WorkspaceModule> workspaceModules = workspaceModules(model);
        if (workspaceModules.isEmpty()) {
            diagnostics.append("\n    <none>");
        } else {
            workspaceModules.values()
                    .forEach(module -> appendWorkspaceModule(diagnostics, module, projectDirectory, buildRoot));
        }

        diagnostics.append("\nReloadable workspace dependencies:");
        if (model.getReloadableWorkspaceDependencies().isEmpty()) {
            diagnostics.append("\n    <none>");
        } else {
            model.getReloadableWorkspaceDependencies().stream()
                    .map(Object::toString)
                    .sorted()
                    .forEach(key -> diagnostics.append("\n    ").append(key));
        }

        diagnostics.append("\nImported platform BOMs:");
        if (model.getPlatforms() == null || model.getPlatforms().getImportedPlatformBoms().isEmpty()) {
            diagnostics.append("\n    <none>");
        } else {
            model.getPlatforms().getImportedPlatformBoms().stream()
                    .map(ArtifactCoords::toGACTVString)
                    .sorted()
                    .forEach(bom -> diagnostics.append("\n    ").append(bom));
        }
        diagnostics.append("\nPlatform property values are omitted from this diagnostics report.");
        return diagnostics.toString();
    }

    private static void appendResolvedDependency(StringBuilder diagnostics, ResolvedDependency dependency,
            Path projectDirectory, Path buildRoot, String indent) {
        diagnostics.append('\n').append(indent).append(dependency.toGACTVString());
        diagnostics.append("\n").append(indent).append("    flags=")
                .append(DependencyFlags.toNames(dependency.getFlags()));
        diagnostics.append("\n").append(indent).append("    resolved-paths:");
        if (dependency.getResolvedPaths() == null || dependency.getResolvedPaths().isEmpty()) {
            diagnostics.append("\n").append(indent).append("        <none>");
        } else {
            dependency.getResolvedPaths().stream()
                    .map(path -> displayPath(path, projectDirectory, buildRoot))
                    .sorted()
                    .forEach(path -> diagnostics.append("\n").append(indent).append("        ").append(path));
        }
        WorkspaceModule workspaceModule = dependency.getWorkspaceModule();
        diagnostics.append("\n").append(indent).append("    workspace-module=")
                .append(workspaceModule == null ? "<none>" : workspaceModule.getId());
        appendDependencies(diagnostics, "resolved-dependencies", dependency.getDependencies(), indent);
        appendDependencies(diagnostics, "direct-dependencies", dependency.getDirectDependencies(), indent);
    }

    private static void appendDependencies(StringBuilder diagnostics, String label,
            Collection<? extends ArtifactCoords> dependencies, String indent) {
        diagnostics.append("\n").append(indent).append("    ").append(label).append(':');
        if (dependencies.isEmpty()) {
            diagnostics.append("\n").append(indent).append("        <none>");
        } else {
            dependencies.stream()
                    .map(ApplicationModelDiagnosticsFormatter::dependencyDescription)
                    .sorted()
                    .forEach(dependency -> diagnostics.append("\n").append(indent).append("        ").append(dependency));
        }
    }

    private static Map<String, WorkspaceModule> workspaceModules(ApplicationModel model) {
        Map<String, WorkspaceModule> modules = new LinkedHashMap<>();
        collectWorkspaceModule(model.getAppArtifact(), modules);
        StreamSupport.stream(model.getDependenciesWithAnyFlag(
                DependencyFlags.RUNTIME_CP | DependencyFlags.DEPLOYMENT_CP | DependencyFlags.COMPILE_ONLY).spliterator(), false)
                .sorted(Comparator.comparing(ArtifactCoords::toGACTVString))
                .forEach(dependency -> collectWorkspaceModule(dependency, modules));
        return modules;
    }

    private static void collectWorkspaceModule(ResolvedDependency dependency, Map<String, WorkspaceModule> modules) {
        WorkspaceModule module = dependency.getWorkspaceModule();
        if (module != null) {
            modules.putIfAbsent(module.getId().toString(), module);
        }
    }

    private static void appendWorkspaceModule(StringBuilder diagnostics, WorkspaceModule module, Path projectDirectory,
            Path buildRoot) {
        diagnostics.append("\n    ").append(module.getId());
        diagnostics.append("\n        module-directory=")
                .append(displayFile(module.getModuleDir(), projectDirectory, buildRoot));
        diagnostics.append("\n        build-directory=")
                .append(displayFile(module.getBuildDir(), projectDirectory, buildRoot));
        diagnostics.append("\n        build-files:");
        if (module.getBuildFiles().isEmpty()) {
            diagnostics.append("\n            <none>");
        } else {
            module.getBuildFiles().stream()
                    .map(path -> displayPath(path, projectDirectory, buildRoot))
                    .sorted()
                    .forEach(path -> diagnostics.append("\n            ").append(path));
        }
        diagnostics.append("\n        parent=").append(module.getParent() == null ? "<none>" : module.getParent().getId());
        appendDependencies(diagnostics, "direct-dependencies", module.getDirectDependencies(), "    ");
        appendDependencies(diagnostics, "direct-dependency-constraints", module.getDirectDependencyConstraints(), "    ");
        diagnostics.append("\n        sources:");
        if (module.getSourceClassifiers().isEmpty()) {
            diagnostics.append("\n            <none>");
        } else {
            module.getSourceClassifiers().stream().sorted().forEach(classifier -> appendArtifactSources(
                    diagnostics, module.getSources(classifier), projectDirectory, buildRoot));
        }
    }

    private static void appendArtifactSources(StringBuilder diagnostics, ArtifactSources sources, Path projectDirectory,
            Path buildRoot) {
        String classifier = sources.getClassifier().isEmpty() ? "main" : sources.getClassifier();
        diagnostics.append("\n            ").append(classifier).append(':');
        appendSourceDirectories(diagnostics, "source-directories", sources.getSourceDirs(), projectDirectory, buildRoot);
        appendSourceDirectories(diagnostics, "resource-directories", sources.getResourceDirs(), projectDirectory,
                buildRoot);
    }

    private static void appendSourceDirectories(StringBuilder diagnostics, String label,
            Collection<SourceDir> directories, Path projectDirectory, Path buildRoot) {
        diagnostics.append("\n                ").append(label).append(':');
        if (directories.isEmpty()) {
            diagnostics.append("\n                    <none>");
        } else {
            directories.stream()
                    .sorted(Comparator.comparing(
                            directory -> displayPath(directory.getDir(), projectDirectory, buildRoot)))
                    .forEach(directory -> {
                        diagnostics.append("\n                    directory=")
                                .append(displayPath(directory.getDir(), projectDirectory, buildRoot));
                        diagnostics.append(" output=")
                                .append(displayPath(directory.getOutputDir(), projectDirectory, buildRoot));
                        diagnostics.append(" generated=")
                                .append(displayPath(directory.getAptSourcesDir(), projectDirectory, buildRoot));
                    });
        }
    }

    private static String dependencyDescription(ArtifactCoords dependency) {
        if (dependency instanceof Dependency declaredDependency) {
            String flags = DependencyFlags.toNames(declaredDependency.getFlags());
            return flags.isEmpty() ? dependency.toGACTVString() : dependency.toGACTVString() + " flags=" + flags;
        }
        return dependency.toGACTVString();
    }

    private static String displayFile(File file, Path projectDirectory, Path buildRoot) {
        return file == null ? "<none>" : displayPath(file.toPath(), projectDirectory, buildRoot);
    }

    private static String displayPath(Path path, Path projectDirectory, Path buildRoot) {
        if (path == null) {
            return "<none>";
        }
        Path normalizedPath = path.toAbsolutePath().normalize();
        Path normalizedProjectDirectory = projectDirectory.toAbsolutePath().normalize();
        String projectPath = relativePath(normalizedPath, normalizedProjectDirectory, "<project>");
        if (projectPath != null) {
            return projectPath;
        }
        String buildPath = relativePath(normalizedPath, buildRoot.toAbsolutePath().normalize(), "<build>");
        if (buildPath != null) {
            return buildPath;
        }
        return portablePath(normalizedPath);
    }

    private static String relativePath(Path path, Path root, String label) {
        try {
            Path relativePath = root.relativize(path);
            if (!relativePath.startsWith("..")) {
                return relativePath.toString().isEmpty() ? label : label + "/" + portablePath(relativePath);
            }
        } catch (IllegalArgumentException ignored) {
            // Paths on different Windows drives cannot be relativized.
        }
        return null;
    }

    private static String portablePath(Path path) {
        return path.toString().replace('\\', '/');
    }
}
