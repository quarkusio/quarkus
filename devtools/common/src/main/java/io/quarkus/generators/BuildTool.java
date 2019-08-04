package io.quarkus.generators;

/**
 * An enum of build tools, such as Maven and Gradle.
 */
public enum BuildTool {

    /** Maven build tool */
    MAVEN("\n# Maven\ntarget/\npom.xml.tag\npom.xml.releaseBackup\npom.xml.versionsBackup\nrelease.properties",
            new String[] { "pom.xml" }),

    /** Gradle build tool */
    GRADLE("\n# Gradle\n.gradle/\nbuild/", new String[] { "build.gradle", "settings.gradle", "gradle.properties" });

    private final String gitIgnoreEntries;

    private final String[] buildFiles;

    private BuildTool(String gitIgnoreEntries, String[] buildFiles) {
        this.gitIgnoreEntries = gitIgnoreEntries;
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

}
