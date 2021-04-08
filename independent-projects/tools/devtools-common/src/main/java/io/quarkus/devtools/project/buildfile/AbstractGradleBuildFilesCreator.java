package io.quarkus.devtools.project.buildfile;

import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.buildfile.AbstractGradleBuildFile.Model;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.platform.tools.ToolsUtils;
import io.quarkus.registry.catalog.ExtensionCatalog;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

abstract class AbstractGradleBuildFilesCreator {

    private static final String GRADLE_PROPERTIES_PATH = "gradle.properties";
    private final QuarkusProject quarkusProject;

    private AtomicReference<Model> modelReference = new AtomicReference<>();

    public AbstractGradleBuildFilesCreator(QuarkusProject quarkusProject) {
        this.quarkusProject = quarkusProject;
    }

    abstract String getSettingsGradlePath();

    abstract String getBuildGradlePath();

    abstract void createBuildContent(String groupId, String version) throws IOException;

    abstract void createSettingsContent(String artifactId) throws IOException;

    abstract void addDependencyInBuildFile(ArtifactCoords coords) throws IOException;

    public void create(String groupId, String artifactId, String version,
            Properties properties, List<ArtifactCoords> extensions) throws IOException {
        createSettingsContent(artifactId);
        createBuildContent(groupId, version);
        createProperties();
        extensions.forEach(e -> {
            try {
                addDependencyInBuildFile(e);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
        this.writeToDisk();
    }

    private void writeToDisk() throws IOException {
        writeToProjectFile(getSettingsGradlePath(), getModel().getSettingsContent().getBytes());
        writeToProjectFile(getBuildGradlePath(), getModel().getBuildContent().getBytes());
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            getModel().getPropertiesContent().store(out, "Gradle properties");
            writeToProjectFile(GRADLE_PROPERTIES_PATH, out.toByteArray());
        }
    }

    protected boolean containsBOM(String groupId, String artifactId) throws IOException {
        String buildContent = getModel().getBuildContent();
        return buildContent.contains("enforcedPlatform(\"${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:")
                || buildContent.contains("enforcedPlatform(\"" + groupId + ":" + artifactId + ":");
    }

    public String getProperty(String propertyName) throws IOException {
        return getModel().getPropertiesContent().getProperty(propertyName);
    }

    QuarkusProject getQuarkusProject() {
        return quarkusProject;
    }

    Model getModel() throws IOException {
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
        if (hasProjectFile(getSettingsGradlePath())) {
            final byte[] settings = readProjectFile(getSettingsGradlePath());
            settingsContent = new String(settings, StandardCharsets.UTF_8);
        }
        if (hasProjectFile(getBuildGradlePath())) {
            final byte[] build = readProjectFile(getBuildGradlePath());
            buildContent = new String(build, StandardCharsets.UTF_8);
        }
        if (hasProjectFile(GRADLE_PROPERTIES_PATH)) {
            final byte[] properties = readProjectFile(GRADLE_PROPERTIES_PATH);
            propertiesContent.load(new ByteArrayInputStream(properties));
        }
        return new Model(settingsContent, buildContent, propertiesContent, null, null);
    }

    protected boolean hasProjectFile(final String fileName) throws IOException {
        final Path filePath = quarkusProject.getProjectDirPath().resolve(fileName);
        return Files.exists(filePath);
    }

    protected byte[] readProjectFile(final String fileName) throws IOException {
        final Path filePath = quarkusProject.getProjectDirPath().resolve(fileName);
        return Files.readAllBytes(filePath);
    }

    protected void writeToProjectFile(final String fileName, final byte[] content) throws IOException {
        Files.write(quarkusProject.getProjectDirPath().resolve(fileName), content);
    }

    private void createProperties() throws IOException {
        final ExtensionCatalog platform = quarkusProject.getExtensionsCatalog();
        Properties props = getModel().getPropertiesContent();
        if (props.getProperty("quarkusPluginVersion") == null) {
            props.setProperty("quarkusPluginVersion",
                    ToolsUtils.getGradlePluginVersion(ToolsUtils.readQuarkusProperties(platform)));
        }
        final ArtifactCoords bom = platform.getBom();
        if (props.getProperty("quarkusPlatformGroupId") == null) {
            props.setProperty("quarkusPlatformGroupId", bom.getGroupId());
        }
        if (props.getProperty("quarkusPlatformArtifactId") == null) {
            props.setProperty("quarkusPlatformArtifactId", bom.getArtifactId());
        }
        if (props.getProperty("quarkusPlatformVersion") == null) {
            props.setProperty("quarkusPlatformVersion", bom.getVersion());
        }
    }

}
