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
import org.apache.maven.model.Model;

import io.quarkus.cli.commands.writer.ProjectWriter;
import io.quarkus.dependencies.Extension;
import io.quarkus.generators.BuildTool;

public class GradleBuildFile extends BuildFile {

    private static final String BUILD_GRADLE_PATH = "build.gradle";
    private static final String SETTINGS_GRADLE_PATH = "settings.gradle";
    private static final String GRADLE_PROPERTIES_PATH = "gradle.properties";

    private Model model;

    public GradleBuildFile(ProjectWriter writer) {
        super(writer, BuildTool.GRADLE);
    }

    @Override
    public void close() throws IOException {
        write(SETTINGS_GRADLE_PATH, getModel().getSettingsContent());
        write(BUILD_GRADLE_PATH, getModel().getBuildContent());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        getModel().getPropertiesContent().store(out, "Gradle properties");
        write(GRADLE_PROPERTIES_PATH, out.toString(StandardCharsets.UTF_8.toString()));
    }

    @Override
    public void completeFile(String groupId, String artifactId, String version)
            throws IOException {
        completeSettingsContent(artifactId);
        completeBuildContent(groupId, version);
        completeProperties();
    }

    private void completeBuildContent(String groupId, String version) throws IOException {
        final String buildContent = getModel().getBuildContent();
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
        getModel().setBuildContent(res.toString());
    }

    private void completeSettingsContent(String artifactId) throws IOException {
        final String settingsContent = getModel().getSettingsContent();
        final StringBuilder res = new StringBuilder(settingsContent);
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
        getModel().setSettingsContent(res.toString());
    }

    private void completeProperties() throws IOException {
        if (getModel().getPropertiesContent().getProperty("quarkusVersion") == null) {
            getModel().getPropertiesContent().setProperty("quarkusVersion", getPluginVersion());
        }
    }

    @Override
    protected void addDependencyInBuildFile(Dependency dependency) throws IOException {
        StringBuilder newBuildContent = new StringBuilder();
        readLineByLine(getModel().getBuildContent(), new AppendDependency(newBuildContent, dependency));
        getModel().setBuildContent(newBuildContent.toString());
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
    protected boolean hasDependency(Extension extension) throws IOException {
        return getDependencies().stream()
                .anyMatch(d -> extension.getGroupId().equals(d.getGroupId())
                        && extension.getArtifactId().equals(d.getArtifactId()));
    }

    @Override
    protected boolean containsBOM() throws IOException {
        return getModel().getBuildContent().contains("enforcedPlatform(\"io.quarkus:quarkus-bom:");
    }

    @Override
    public List<Dependency> getDependencies() throws IOException {
        return Collections.emptyList();
    }

    @Override
    public String getProperty(String propertyName) throws IOException {
        return getModel().getPropertiesContent().getProperty(propertyName);
    }

    @Override
    protected List<Dependency> getManagedDependencies() {
        // Gradle tooling API only provide resolved dependencies.
        return Collections.emptyList();
    }

    private Model getModel() throws IOException {
        if (model == null) {
            initModel();
        }
        return model;
    }

    protected void initModel() throws IOException {
        String settingsContent = "";
        String buildContent = "";
        Properties propertiesContent = new Properties();
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
        this.model = new Model(settingsContent, buildContent, propertiesContent);
    }

    protected String getBuildContent() throws IOException {
        return getModel().getBuildContent();
    }

    private class Model {
        private String settingsContent;
        private String buildContent;
        private Properties propertiesContent;

        public Model(String settingsContent, String buildContent, Properties propertiesContent) {
            this.settingsContent = settingsContent;
            this.buildContent = buildContent;
            this.propertiesContent = propertiesContent;
        }

        public String getSettingsContent() {
            return settingsContent;
        }

        public String getBuildContent() {
            return buildContent;
        }

        public Properties getPropertiesContent() {
            return propertiesContent;
        }

        public void setSettingsContent(String settingsContent) {
            this.settingsContent = settingsContent;
        }

        public void setBuildContent(String buildContent) {
            this.buildContent = buildContent;
        }

        public void setPropertiesContent(Properties propertiesContent) {
            this.propertiesContent = propertiesContent;
        }
    }
}
