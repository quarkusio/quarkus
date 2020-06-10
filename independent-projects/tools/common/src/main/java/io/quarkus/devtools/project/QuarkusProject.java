package io.quarkus.devtools.project;

import static com.google.common.base.Preconditions.checkNotNull;

import io.quarkus.devtools.project.buildfile.MavenBuildFile;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import java.nio.file.Path;

public final class QuarkusProject {

    private final Path projectFolderPath;
    private final QuarkusPlatformDescriptor platformDescriptor;
    private final ExtensionManager extensionManager;

    private QuarkusProject(final Path projectFolderPath, final QuarkusPlatformDescriptor platformDescriptor,
            final ExtensionManager extensionManager) {
        this.projectFolderPath = checkNotNull(projectFolderPath, "projectFolderPath is required");
        this.platformDescriptor = checkNotNull(platformDescriptor, "platformDescriptor is required");
        this.extensionManager = checkNotNull(extensionManager, "extensionManager is required");
    }

    public static QuarkusProject of(final Path projectFolderPath, final QuarkusPlatformDescriptor platformDescriptor,
            final ExtensionManager extensionManager) {
        return new QuarkusProject(projectFolderPath, platformDescriptor, extensionManager);
    }

    public static QuarkusProject of(final Path projectFolderPath, final QuarkusPlatformDescriptor platformDescriptor,
            final BuildTool buildTool) {
        return new QuarkusProject(projectFolderPath, platformDescriptor,
                buildTool.createExtensionManager(projectFolderPath, platformDescriptor));
    }

    public static QuarkusProject maven(final Path projectFolderPath, final QuarkusPlatformDescriptor platformDescriptor) {
        return new QuarkusProject(projectFolderPath, platformDescriptor,
                new MavenBuildFile(projectFolderPath, platformDescriptor));
    }

    public static QuarkusProject resolveExistingProject(final Path projectFolderPath,
            final QuarkusPlatformDescriptor descriptor) {
        final BuildTool buildTool = resolveExistingProjectBuildTool(projectFolderPath);
        if (buildTool == null) {
            throw new IllegalStateException("This is neither a Maven or Gradle project");
        }
        return of(projectFolderPath, descriptor, buildTool);
    }

    public Path getProjectFolderPath() {
        return projectFolderPath;
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

    public static BuildTool resolveExistingProjectBuildTool(Path projectFolderPath) {
        if (projectFolderPath.resolve("pom.xml").toFile().exists()) {
            return BuildTool.MAVEN;
        } else if (projectFolderPath.resolve("build.gradle").toFile().exists()
                || projectFolderPath.resolve("build.gradle.kts").toFile().exists()) {
            return BuildTool.GRADLE;
        }
        return null;
    }

}
