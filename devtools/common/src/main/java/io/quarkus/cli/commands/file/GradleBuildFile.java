package io.quarkus.cli.commands.file;

import static io.quarkus.maven.utilities.MojoUtils.getPluginVersion;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.maven.model.Dependency;

import io.quarkus.cli.commands.writer.ProjectWriter;
import io.quarkus.dependencies.Extension;
import io.quarkus.maven.utilities.MojoUtils;

public class GradleBuildFile extends BuildFile {

    private static final String BUILD_GRADLE_PATH = "build.gradle";
    private static final String SETTINGS_GRADLE_PATH = "settings.gradle";
    private static final String GRADLE_PROPERTIES_PATH = "gradle.properties";

    private String settingsContent = "";
    private String buildContent = "";
    private String propertiesContent = "";
    private ArrayList<Dependency> dependencies = null;

    public GradleBuildFile(ProjectWriter writer) throws IOException {
        super(writer);
        if (writer.exists(SETTINGS_GRADLE_PATH)) {
            final byte[] settings = writer.getContent(SETTINGS_GRADLE_PATH);
            settingsContent = new String(settings, StandardCharsets.UTF_8);
        }
        if (writer.exists(BUILD_GRADLE_PATH)) {
            final byte[] build = writer.getContent(BUILD_GRADLE_PATH);
            buildContent = new String(build, StandardCharsets.UTF_8);
        }
        if (writer.exists(GRADLE_PROPERTIES_PATH)) {
            final byte[] properties = writer.getContent(GRADLE_PROPERTIES_PATH);
            propertiesContent = new String(properties, StandardCharsets.UTF_8);
        }
    }

    @Override
    public void write() throws IOException {
        write(SETTINGS_GRADLE_PATH, settingsContent);
        write(BUILD_GRADLE_PATH, buildContent);
        write(GRADLE_PROPERTIES_PATH, propertiesContent);
    }

    public void completeFile(String groupId, String artifactId, String version) throws IOException {
        completeSettingsContent(artifactId);
        completeBuildContent(groupId, version);
        completeProperties(artifactId);
        write();
    }

    private void completeBuildContent(String groupId, String version) {
        StringBuilder res = new StringBuilder(buildContent);
        if (!buildContent.contains("io.quarkus:quarkus-gradle-plugin")) {
            res.append(System.lineSeparator());
            res.append("buildscript {").append(System.lineSeparator());
            res.append("    repositories {").append(System.lineSeparator());
            res.append("        mavenLocal()").append(System.lineSeparator());
            res.append("    }").append(System.lineSeparator());
            res.append("    dependencies {").append(System.lineSeparator());
            res.append("        classpath \"io.quarkus:quarkus-gradle-plugin:").append(getPluginVersion()).append("\"")
                    .append(System.lineSeparator());
            res.append("    }").append(System.lineSeparator());
            res.append("}").append(System.lineSeparator());
        }
        if (!buildContent.contains("apply plugin: 'io.quarkus'") && !buildContent.contains("id 'io.quarkus'")) {
            res.append(System.lineSeparator()).append("apply plugin: 'io.quarkus'").append(System.lineSeparator());
        }
        if (!containsBOM()) {
            res.append(System.lineSeparator());
            res.append("dependencies {").append(System.lineSeparator());
            res.append("    implementation enforcedPlatform(\"io.quarkus:quarkus-bom:${quarkusVersion}\")")
                    .append(System.lineSeparator());
            res.append("    implementation 'io.quarkus:quarkus-resteasy'").append(System.lineSeparator());
            res.append("    testImplementation 'io.quarkus:quarkus-junit5'").append(System.lineSeparator());
            res.append("    testImplementation 'io.rest-assured:rest-assured'").append(System.lineSeparator());
            res.append("}").append(System.lineSeparator());

        }
        String groupLine = "group '" + groupId + "'";
        if (!buildContent.contains(groupLine)) {
            res.append(System.lineSeparator()).append(groupLine)
                    .append(System.lineSeparator());
        }
        String versionLine = "version '" + version + "'";
        if (!buildContent.contains(versionLine)) {
            res.append(System.lineSeparator()).append(versionLine)
                    .append(System.lineSeparator());
        }
        buildContent = res.toString();
    }

    private void completeSettingsContent(String artifactId) {
        StringBuilder res = new StringBuilder(settingsContent);
        if (!settingsContent.contains("io.quarkus:quarkus-gradle-plugin")) {
            res.append(System.lineSeparator());
            res.append("pluginManagement {").append(System.lineSeparator());
            res.append("    repositories {").append(System.lineSeparator());
            res.append("        mavenLocal()").append(System.lineSeparator());
            res.append("        mavenCentral()").append(System.lineSeparator());
            res.append("        gradlePluginPortal()").append(System.lineSeparator());
            res.append("    }").append(System.lineSeparator());
            res.append("    resolutionStrategy {").append(System.lineSeparator());
            res.append("        eachPlugin {").append(System.lineSeparator());
            res.append("            if (requested.id.id == 'io.quarkus') {").append(System.lineSeparator());
            res.append("                useModule(\"io.quarkus:quarkus-gradle-plugin:${quarkusVersion}\")")
                    .append(System.lineSeparator());
            res.append("            }").append(System.lineSeparator());
            res.append("        }").append(System.lineSeparator());
            res.append("    }").append(System.lineSeparator());
            res.append("}").append(System.lineSeparator());
        }
        if (!settingsContent.contains("rootProject.name")) {
            res.append(System.lineSeparator()).append("rootProject.name='").append(artifactId).append("'")
                    .append(System.lineSeparator());
        }
        settingsContent = res.toString();
    }

    private void completeProperties(String artifactId) {
        StringBuilder res = new StringBuilder(propertiesContent);
        if (!propertiesContent.contains("quarkusVersion = ")) {
            res.append(System.lineSeparator()).append("quarkusVersion = ").append(getPluginVersion()).append(artifactId)
                    .append(System.lineSeparator());
        }
        propertiesContent = res.toString();
    }

    @Override
    protected void addDependencyInBuildFile(Dependency dependency) {
        StringBuilder newBuildContent = new StringBuilder();
        try (Scanner scanner = new Scanner(new ByteArrayInputStream(buildContent.getBytes(StandardCharsets.UTF_8)))) {
            while (scanner.hasNextLine()) {
                String currentLine = scanner.nextLine();
                newBuildContent.append(currentLine).append(System.lineSeparator());
                if (currentLine.startsWith("dependencies {")) {
                    newBuildContent.append("    implementation '")
                            .append(dependency.getGroupId())
                            .append(":")
                            .append(dependency.getArtifactId())
                            .append("'")
                            .append(System.lineSeparator());
                }
            }
        }
        buildContent = newBuildContent.toString();
    }

    @Override
    protected boolean hasDependency(Extension extension) {
        return getDependencies().stream()
                .anyMatch(d -> extension.getGroupId().equals(d.getGroupId())
                        && extension.getArtifactId().equals(d.getArtifactId()));
    }

    @Override
    protected boolean containsBOM() {
        return buildContent.contains("enforcedPlatform(\"io.quarkus:quarkus-bom:");
    }

    @Override
    protected List<Dependency> getDependencies() {
        if (dependencies == null) {
            dependencies = new ArrayList<>();
            boolean inDependencies = false;
            try (Scanner scanner = new Scanner(new ByteArrayInputStream(buildContent.getBytes(StandardCharsets.UTF_8)))) {
                while (scanner.hasNextLine()) {
                    String currentLine = scanner.nextLine();
                    if (currentLine.startsWith("dependencies {")) {
                        inDependencies = true;
                    } else if (currentLine.startsWith("}")) {
                        inDependencies = false;
                    } else if (inDependencies && currentLine.contains("implementation ")
                            && !currentLine.contains("enforcedPlatform")) {
                        if (currentLine.indexOf('\'') != -1) {
                            String dep = currentLine.substring(currentLine.indexOf('\'') + 1, currentLine.lastIndexOf('\''));
                            dependencies.add(MojoUtils.parse(dep.trim().toLowerCase()));
                        }
                    }
                }
            }
        }
        return dependencies;
    }

}
