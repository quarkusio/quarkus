package io.quarkus.devtools.project;

import static java.util.Objects.requireNonNull;

import io.quarkus.devtools.project.buildfile.MavenBuildFile;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import java.nio.file.Path;

public final class QuarkusProject {

    private final Path projectDirPath;
    private final QuarkusPlatformDescriptor platformDescriptor;
    private final ExtensionManager extensionManager;

    private QuarkusProject(final Path projectDirPath, final QuarkusPlatformDescriptor platformDescriptor,
            final ExtensionManager extensionManager) {
        this.projectDirPath = requireNonNull(projectDirPath, "projectDirPath is required");
        this.platformDescriptor = requireNonNull(platformDescriptor, "platformDescriptor is required");
        this.extensionManager = requireNonNull(extensionManager, "extensionManager is required");
    }

    public static QuarkusProject of(final Path projectDirPath, final QuarkusPlatformDescriptor platformDescriptor,
            final ExtensionManager extensionManager) {
        return new QuarkusProject(projectDirPath, platformDescriptor, extensionManager);
    }

    public static QuarkusProject of(final Path projectDirPath, final QuarkusPlatformDescriptor platformDescriptor,
            final BuildTool buildTool) {
        return new QuarkusProject(projectDirPath, platformDescriptor,
                buildTool.createExtensionManager(projectDirPath, platformDescriptor));
    }

    public static QuarkusProject maven(final Path projectDirPath, final QuarkusPlatformDescriptor platformDescriptor) {
        return new QuarkusProject(projectDirPath, platformDescriptor,
                new MavenBuildFile(projectDirPath, platformDescriptor));
    }

    public static QuarkusProject resolveExistingProject(final Path projectDirPath,
            final QuarkusPlatformDescriptor descriptor) {
        final BuildTool buildTool = resolveExistingProjectBuildTool(projectDirPath);
        if (buildTool == null) {
            throw new IllegalStateException("This is neither a Maven or Gradle project");
        }
        return of(projectDirPath, descriptor, buildTool);
    }

    public Path getProjectDirPath() {
        return projectDirPath;
    }

    public BuildTool getBuildTool() {
        return extensionManager.getBuildTool();
    }

    public ExtensionManager getExtensionManager() {
        return extensionManager;
    }

    public QuarkusPlatformDescriptor getPlatformDescriptor() {
        return platformDescriptor;
    }

    public static BuildTool resolveExistingProjectBuildTool(Path projectDirPath) {
        if (projectDirPath.resolve("pom.xml").toFile().exists()) {
            return BuildTool.MAVEN;
        } else if (projectDirPath.resolve("build.gradle").toFile().exists()) {
            return BuildTool.GRADLE;
        } else if (projectDirPath.resolve("build.gradle.kts").toFile().exists()) {
            return BuildTool.GRADLE_KOTLIN_DSL;
        }
        return null;
    }

}
