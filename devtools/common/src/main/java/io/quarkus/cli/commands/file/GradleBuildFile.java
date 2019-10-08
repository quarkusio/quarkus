package io.quarkus.cli.commands.file;

import static io.quarkus.maven.utilities.MojoUtils.getPluginVersion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.function.Consumer;

import org.apache.maven.model.Dependency;

import io.quarkus.cli.commands.writer.ProjectWriter;
import io.quarkus.dependencies.Extension;
import io.quarkus.generators.BuildTool;

public class GradleBuildFile extends BuildFile {

    protected static final String BUILD_GRADLE_PATH = "build.gradle";
    private static final String SETTINGS_GRADLE_PATH = "settings.gradle";
    private static final String GRADLE_PROPERTIES_PATH = "gradle.properties";

    private String settingsContent = "";
    private String buildContent = "";
    private Properties propertiesContent = new Properties();

    public GradleBuildFile(ProjectWriter writer) throws IOException {
        super(writer, BuildTool.GRADLE);
    }

    @Override
    public void close() throws IOException {
        write(SETTINGS_GRADLE_PATH, settingsContent);
        write(BUILD_GRADLE_PATH, buildContent);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        propertiesContent.store(out, "Gradle properties");
        write(GRADLE_PROPERTIES_PATH, out.toString(StandardCharsets.UTF_8.toString()));
    }

    @Override
    public void completeFile(String groupId, String artifactId, String version)
            throws IOException {
        init();
        completeSettingsContent(artifactId);
        completeBuildContent(groupId, version);
        completeProperties();
    }

    protected void init() throws IOException {
        if (getWriter().exists(SETTINGS_GRADLE_PATH)) {
            final byte[] settings = getWriter().getContent(SETTINGS_GRADLE_PATH);
            settingsContent = new String(settings, StandardCharsets.UTF_8);
        }
        if (getWriter().exists(BUILD_GRADLE_PATH)) {
            final byte[] build = getWriter().getContent(BUILD_GRADLE_PATH);
            buildContent = new String(build, StandardCharsets.UTF_8);
        }
        if (getWriter().exists(GRADLE_PROPERTIES_PATH)) {
            final byte[] properties = getWriter().getContent(GRADLE_PROPERTIES_PATH);
            propertiesContent.load(new ByteArrayInputStream(properties));
        }
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

    private void completeProperties() {
        if (propertiesContent.getProperty("quarkusVersion") == null) {
            propertiesContent.setProperty("quarkusVersion", getPluginVersion());
        }
    }

    @Override
    protected void addDependencyInBuildFile(Dependency dependency) {
        StringBuilder newBuildContent = new StringBuilder();
        readLineByLine(buildContent, new AppendDependency(newBuildContent, dependency));
        buildContent = newBuildContent.toString();
    }

    private void readLineByLine(String content, Consumer<String> lineConsumer) {
        try (Scanner scanner = new Scanner(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))) {
            while (scanner.hasNextLine()) {
                String currentLine = scanner.nextLine();
                lineConsumer.accept(currentLine);
            }
        }
    }

    private static class AppendDependency implements Consumer<String> {

        private StringBuilder newContent;
        private Dependency dependency;

        public AppendDependency(StringBuilder newContent, Dependency dependency) {
            this.newContent = newContent;
            this.dependency = dependency;
        }

        @Override
        public void accept(String currentLine) {
            newContent.append(currentLine).append(System.lineSeparator());
            if (currentLine.startsWith("dependencies {")) {
                newContent.append("    implementation '")
                        .append(dependency.getGroupId())
                        .append(":")
                        .append(dependency.getArtifactId());
                if (dependency.getVersion() != null && !dependency.getVersion().isEmpty()) {
                    newContent.append(":")
                            .append(dependency.getVersion());
                }
                newContent.append("'")
                        .append(System.lineSeparator());
            }
        }

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
    public List<Dependency> getDependencies() {
        return Collections.emptyList();
    }

    @Override
    public String getProperty(String propertyName) {
        return propertiesContent.getProperty(propertyName);
    }

    @Override
    protected List<Dependency> getManagedDependencies() {
        // Gradle tooling API only provide resolved dependencies.
        return Collections.emptyList();
    }

    protected String getBuildContent() {
        return buildContent;
    }
}
