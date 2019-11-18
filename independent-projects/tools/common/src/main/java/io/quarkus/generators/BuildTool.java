package io.quarkus.generators;

import java.io.IOException;

import io.quarkus.cli.commands.file.BuildFile;
import io.quarkus.cli.commands.file.GradleBuildFile;
import io.quarkus.cli.commands.file.MavenBuildFile;
import io.quarkus.cli.commands.writer.ProjectWriter;

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

    public BuildFile createBuildFile(final ProjectWriter writer) throws IOException {
        switch (this) {
            case GRADLE:
                return new GradleBuildFile(writer);
            case MAVEN:
            default:
                return new MavenBuildFile(writer);
        }
    }
}
