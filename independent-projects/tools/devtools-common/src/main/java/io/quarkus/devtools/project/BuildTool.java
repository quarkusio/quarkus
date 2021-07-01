package io.quarkus.devtools.project;

import io.quarkus.devtools.project.buildfile.GroovyGradleBuildFile;
import io.quarkus.devtools.project.buildfile.KotlinGradleBuildFile;
import io.quarkus.devtools.project.buildfile.MavenBuildFile;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.registry.catalog.ExtensionCatalog;
import java.nio.file.Path;
import java.util.Locale;

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
            new String[] { "build.gradle", "settings.gradle", "gradle.properties" }),

    /** Gradle build tool with Kotlin DSL */
    GRADLE_KOTLIN_DSL("\n# Gradle\n.gradle/\nbuild/",
            "build",
            new String[] { "build.gradle.kts", "settings.gradle.kts", "gradle.properties" }),

    /** JBang build tool */
    JBANG("\n# JBang\n.target/\nbuild/",
            "build",
            new String[0]);

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

    public ExtensionManager createExtensionManager(final Path projectDirPath,
            ExtensionCatalog catalog) {
        switch (this) {
            case GRADLE:
                return new GroovyGradleBuildFile();
            case GRADLE_KOTLIN_DSL:
                return new KotlinGradleBuildFile();
            case MAVEN:
            default:
                return new MavenBuildFile(projectDirPath, catalog);
        }
    }

    public String getKey() {
        return toString().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public static BuildTool resolveExistingProject(Path path) {
        return QuarkusProject.resolveExistingProjectBuildTool(path);
    }

    public static BuildTool findTool(String tool) {
        for (BuildTool value : BuildTool.values()) {
            if (value.toString().equalsIgnoreCase(tool) || value.getKey().equalsIgnoreCase(tool)) {
                return value;
            }
        }
        return null;
    }
}
