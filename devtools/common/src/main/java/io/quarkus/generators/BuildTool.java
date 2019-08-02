package io.quarkus.generators;

/**
 * An enum of build tools, such as Maven and Gradle.
 */
public enum BuildTool {

    /** Maven build tool */
    MAVEN("\n# Maven\ntarget/\npom.xml.tag\npom.xml.releaseBackup\npom.xml.versionsBackup\nrelease.properties"),

    /** Gradle build tool */
    GRADLE("\n# Gradle\n.gradle/\nbuild/");

    private final String gitIgnoreEntries;

    private BuildTool(String gitIgnoreEntries) {
        this.gitIgnoreEntries = gitIgnoreEntries;
    }

    /**
     * @return {@code \n}-separated lines to add to a {@code .gitignore} file
     */
    public String getGitIgnoreEntries() {
        return gitIgnoreEntries;
    }
}
