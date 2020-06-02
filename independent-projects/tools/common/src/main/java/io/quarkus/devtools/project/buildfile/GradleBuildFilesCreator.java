package io.quarkus.devtools.project.buildfile;

import static io.quarkus.devtools.project.buildfile.AbstractGradleBuildFile.addDependencyInModel;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.buildfile.AbstractGradleBuildFile.Model;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.tools.ToolsUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class GradleBuildFilesCreator {

    private static final String BUILD_GRADLE_PATH = "build.gradle";
    private static final String SETTINGS_GRADLE_PATH = "settings.gradle";
    private static final String GRADLE_PROPERTIES_PATH = "gradle.properties";
    private final QuarkusProject quarkusProject;

    private AtomicReference<Model> modelReference = new AtomicReference<>();

    public GradleBuildFilesCreator(QuarkusProject quarkusProject) {
        this.quarkusProject = quarkusProject;
    }

    public void create(String groupId, String artifactId, String version,
            Properties properties, List<AppArtifactCoords> extensions) throws IOException {
        createSettingsContent(artifactId);
        createBuildContent(groupId, version);
        createProperties();
        extensions.stream()
                .forEach(e -> {
                    try {
                        addDependencyInBuildFile(e);
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                });
        this.writeToDisk();
    }

    private void writeToDisk() throws IOException {
        writeToProjectFile(SETTINGS_GRADLE_PATH, getModel().getSettingsContent().getBytes());
        writeToProjectFile(BUILD_GRADLE_PATH, getModel().getBuildContent().getBytes());
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            getModel().getPropertiesContent().store(out, "Gradle properties");
            writeToProjectFile(GRADLE_PROPERTIES_PATH, out.toByteArray());
        }
    }

    private void addDependencyInBuildFile(AppArtifactCoords coords) throws IOException {
        addDependencyInModel(getModel(), coords);
    }

    protected boolean containsBOM(String groupId, String artifactId) throws IOException {
        String buildContent = getModel().getBuildContent();
        return buildContent.contains("enforcedPlatform(\"${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:")
                || buildContent.contains("enforcedPlatform(\"" + groupId + ":" + artifactId + ":");
    }

    public String getProperty(String propertyName) throws IOException {
        return getModel().getPropertiesContent().getProperty(propertyName);
    }

    private void readLineByLine(String content, Consumer<String> lineConsumer) {
        try (Scanner scanner = new Scanner(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8.name())) {
            while (scanner.hasNextLine()) {
                String currentLine = scanner.nextLine();
                lineConsumer.accept(currentLine);
            }
        }
    }

    private Model getModel() throws IOException {
        return modelReference.updateAndGet(model -> {
            if (model == null) {
                try {
                    return readModel();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            return model;
        });
    }

    private Model readModel() throws IOException {
        String settingsContent = "";
        String buildContent = "";
        Properties propertiesContent = new Properties();
        if (hasProjectFile(SETTINGS_GRADLE_PATH)) {
            final byte[] settings = readProjectFile(SETTINGS_GRADLE_PATH);
            settingsContent = new String(settings, StandardCharsets.UTF_8);
        }
        if (hasProjectFile(BUILD_GRADLE_PATH)) {
            final byte[] build = readProjectFile(BUILD_GRADLE_PATH);
            buildContent = new String(build, StandardCharsets.UTF_8);
        }
        if (hasProjectFile(GRADLE_PROPERTIES_PATH)) {
            final byte[] properties = readProjectFile(GRADLE_PROPERTIES_PATH);
            propertiesContent.load(new ByteArrayInputStream(properties));
        }
        return new Model(settingsContent, buildContent, propertiesContent, null, null);
    }

    protected boolean hasProjectFile(final String fileName) throws IOException {
        final Path filePath = quarkusProject.getProjectFolderPath().resolve(fileName);
        return Files.exists(filePath);
    }

    protected byte[] readProjectFile(final String fileName) throws IOException {
        final Path filePath = quarkusProject.getProjectFolderPath().resolve(fileName);
        return Files.readAllBytes(filePath);
    }

    protected void writeToProjectFile(final String fileName, final byte[] content) throws IOException {
        Files.write(quarkusProject.getProjectFolderPath().resolve(fileName), content);
    }

    private void createBuildContent(String groupId, String version)
            throws IOException {
        final String buildContent = getModel().getBuildContent();
        StringBuilder res = new StringBuilder(buildContent);
        if (!buildContent.contains("id 'io.quarkus'")) {
            res.append("plugins {");
            res.append(System.lineSeparator()).append("    id 'java'").append(System.lineSeparator());
            res.append(System.lineSeparator()).append("    id 'io.quarkus'").append(System.lineSeparator());
            res.append("}");
        }
        if (!containsBOM(quarkusProject.getPlatformDescriptor().getBomGroupId(),
                quarkusProject.getPlatformDescriptor().getBomArtifactId())) {
            res.append(System.lineSeparator());
            res.append("dependencies {").append(System.lineSeparator());
            res.append(
                    "    implementation enforcedPlatform(\"${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}\")")
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

        res.append(System.lineSeparator())
                .append("test {").append(System.lineSeparator())
                .append("    systemProperty \"java.util.logging.manager\", \"org.jboss.logmanager.LogManager\"")
                .append(System.lineSeparator())
                .append("}");

        getModel().setBuildContent(res.toString());
    }

    private void createSettingsContent(String artifactId) throws IOException {
        final String settingsContent = getModel().getSettingsContent();
        final StringBuilder res = new StringBuilder();
        if (!settingsContent.contains("id 'io.quarkus'")) {
            res.append(System.lineSeparator());
            res.append("pluginManagement {").append(System.lineSeparator());
            res.append("    repositories {").append(System.lineSeparator());
            res.append("        mavenLocal()").append(System.lineSeparator());
            res.append("        mavenCentral()").append(System.lineSeparator());
            res.append("        gradlePluginPortal()").append(System.lineSeparator());
            res.append("    }").append(System.lineSeparator());
            res.append("    plugins {").append(System.lineSeparator());
            res.append("        id 'io.quarkus' version \"${quarkusPluginVersion}\"").append(System.lineSeparator());
            res.append("    }").append(System.lineSeparator());
            res.append("}").append(System.lineSeparator());
        }
        if (!settingsContent.contains("rootProject.name")) {
            res.append(System.lineSeparator()).append("rootProject.name='").append(artifactId).append("'")
                    .append(System.lineSeparator());
        }
        res.append(settingsContent);
        getModel().setSettingsContent(res.toString());
    }

    private void createProperties() throws IOException {
        final QuarkusPlatformDescriptor platform = quarkusProject.getPlatformDescriptor();
        Properties props = getModel().getPropertiesContent();
        if (props.getProperty("quarkusPluginVersion") == null) {
            props.setProperty("quarkusPluginVersion", ToolsUtils.getPluginVersion(ToolsUtils.readQuarkusProperties(platform)));
        }
        if (props.getProperty("quarkusPlatformGroupId") == null) {
            props.setProperty("quarkusPlatformGroupId", platform.getBomGroupId());
        }
        if (props.getProperty("quarkusPlatformArtifactId") == null) {
            props.setProperty("quarkusPlatformArtifactId", platform.getBomArtifactId());
        }
        if (props.getProperty("quarkusPlatformVersion") == null) {
            props.setProperty("quarkusPlatformVersion", platform.getBomVersion());
        }
    }

}
