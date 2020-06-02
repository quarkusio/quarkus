package io.quarkus.devtools.project;

import io.quarkus.devtools.project.buildfile.GenericGradleBuildFile;
import io.quarkus.devtools.project.buildfile.MavenBuildFile;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import java.nio.file.Path;

/**
 * An enum of build tools, such as Maven and Gradle.
 */
public enum BuildTool {

    /** Maven build tool */
    MAVEN("\n# Maven\ntarget/\npom.xml.tag\npom.xml.releaseBackup\npom.xml.versionsBackup\nrelease.properties",
            "target",
            new String[] { "pom.xml" }),

    /** Gradle build tool */
    GRADLE("\n# Gradle\n.gradle/\nbuild/",
            "build",
            new String[] { "build.gradle", "settings.gradle", "gradle.properties" });

    private final String gitIgnoreEntries;

    private final String buildDirectory;

    private final String[] buildFiles;

    private BuildTool(String gitIgnoreEntries, String buildDirectory, String[] buildFiles) {
        this.gitIgnoreEntries = gitIgnoreEntries;
        this.buildDirectory = buildDirectory;
        this.buildFiles = buildFiles;
    }

    /**
     * @return {@code \n}-separated lines to add to a {@code .gitignore} file
     */
    public String getGitIgnoreEntries() {
        return gitIgnoreEntries;
    }

    public String[] getBuildFiles() {
        return buildFiles;
    }

    public String getDependenciesFile() {
        return buildFiles[0];
    }

    public String getBuildDirectory() {
        return buildDirectory;
    }

    public ExtensionManager createExtensionManager(final Path projectFolderPath,
            final QuarkusPlatformDescriptor platformDescriptor) {
        switch (this) {
            case GRADLE:
                return new GenericGradleBuildFile();
            case MAVEN:
            default:
                return new MavenBuildFile(projectFolderPath, platformDescriptor);
        }
    }
}
