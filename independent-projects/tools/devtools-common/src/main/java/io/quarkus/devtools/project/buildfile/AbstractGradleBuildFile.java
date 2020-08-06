package io.quarkus.devtools.project.buildfile;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

// We keep it here to take advantage of the abstract tests
public abstract class AbstractGradleBuildFile extends BuildFile {

    private static final String BUILD_GRADLE_PATH = "build.gradle";
    private static final String SETTINGS_GRADLE_PATH = "settings.gradle";
    private static final String GRADLE_PROPERTIES_PATH = "gradle.properties";

    private final Path rootProjectPath;

    private AtomicReference<Model> modelReference = new AtomicReference<>();

    public AbstractGradleBuildFile(final Path projectDirPath, final QuarkusPlatformDescriptor platformDescriptor) {
        this(projectDirPath, platformDescriptor, null);
    }

    public AbstractGradleBuildFile(final Path projectDirPath, final QuarkusPlatformDescriptor platformDescriptor,
            Path rootProjectPath) {
        super(projectDirPath, platformDescriptor);
        this.rootProjectPath = rootProjectPath;
    }

    @Override
    public void writeToDisk() throws IOException {
        if (rootProjectPath != null) {
            Files.write(rootProjectPath.resolve(SETTINGS_GRADLE_PATH), getModel().getRootSettingsContent().getBytes());
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                getModel().getRootPropertiesContent().store(out, "Gradle properties");
                Files.write(rootProjectPath.resolve(GRADLE_PROPERTIES_PATH),
                        out.toByteArray());
            }
        } else {
            writeToProjectFile(SETTINGS_GRADLE_PATH, getModel().getSettingsContent().getBytes());
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                getModel().getPropertiesContent().store(out, "Gradle properties");
                writeToProjectFile(GRADLE_PROPERTIES_PATH, out.toByteArray());
            }
        }
        writeToProjectFile(BUILD_GRADLE_PATH, getModel().getBuildContent().getBytes());
    }

    @Override
    protected boolean addDependency(AppArtifactCoords coords, boolean managed) {
        return addDependencyInModel(getModel(), coords, managed);
    }

    static boolean addDependencyInModel(Model model, AppArtifactCoords coords, boolean managed) {
        boolean isBOM = "pom".equals(coords.getType());
        StringBuilder newDependency;
        if (isBOM) {
            // Check if BOM is not included already
            String resolvedPlatform = String
                    .format("%s:%s:%s", getProperty(model, "quarkusPlatformGroupId"),
                            getProperty(model, "quarkusPlatformArtifactId"),
                            getProperty(model, "quarkusPlatformVersion"));
            String thisBOM = String.format("%s:%s:%s", coords.getGroupId(), coords.getArtifactId(), coords.getVersion());
            if (thisBOM.equals(resolvedPlatform)) {
                // BOM matches the platform, no need to do anything
                return false;
            }
            newDependency = new StringBuilder()
                    .append("    implementation enforcedPlatform(\"")
                    .append(thisBOM)
                    .append("\")'");
        } else {
            newDependency = new StringBuilder()
                    .append("    implementation '")
                    .append(coords.getGroupId())
                    .append(":")
                    .append(coords.getArtifactId());
        }
        if (!managed &&
                (coords.getVersion() != null && !coords.getVersion().isEmpty())) {
            newDependency.append(":").append(coords.getVersion());
        }
        newDependency.append("'").append(System.lineSeparator());
        String newDependencyString = newDependency.toString();
        StringBuilder buildContent = new StringBuilder(model.getBuildContent());
        boolean changed = false;
        if (buildContent.indexOf(newDependencyString) == -1) {
            changed = true;
            // Add dependency after "dependencies {"
            int indexOfDeps = buildContent.indexOf("dependencies {");
            if (indexOfDeps > -1) {
                // The line below fails on Windows if System.lineSeparator() is used
                int nextLine = buildContent.indexOf("\n", indexOfDeps) + 1;
                buildContent.insert(nextLine, newDependencyString);
            } else {
                // if no "dependencies {" found, add one
                buildContent.append("dependencies {").append(System.lineSeparator())
                        .append(newDependencyString)
                        .append("}").append(System.lineSeparator());
            }
            model.setBuildContent(buildContent.toString());
        }
        return changed;
    }

    @Override
    protected void removeDependency(AppArtifactKey key) {
        StringBuilder newBuildContent = new StringBuilder();
        Scanner scanner = new Scanner(getModel().getBuildContent());
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (!line.contains(key.getGroupId() + ":" + key.getArtifactId())) {
                newBuildContent.append(line).append(System.lineSeparator());
            }
        }
        scanner.close();
        getModel().setBuildContent(newBuildContent.toString());
    }

    @Override
    public String getProperty(String propertyName) {
        return getProperty(getModel(), propertyName);
    }

    @Override
    public BuildTool getBuildTool() {
        return BuildTool.GRADLE;
    }

    static String getProperty(Model model, String propertyName) {
        final String property = model.getPropertiesContent().getProperty(propertyName);
        if (property != null || model.getRootPropertiesContent() == null) {
            return property;
        }
        return model.getRootPropertiesContent().getProperty(propertyName);
    }

    private Model getModel() {
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

    @Override
    protected void refreshData() {
        this.modelReference.set(null);
    }

    private boolean hasRootProjectFile(final String fileName) throws IOException {
        if (rootProjectPath == null) {
            return false;
        }
        final Path filePath = rootProjectPath.resolve(fileName);
        return Files.exists(filePath);
    }

    private byte[] readRootProjectFile(final String fileName) throws IOException {
        if (rootProjectPath == null) {
            throw new IllegalStateException("There is no rootProject defined in this GradleBuildFile");
        }
        final Path filePath = rootProjectPath.resolve(fileName);
        return Files.readAllBytes(filePath);
    }

    private Model readModel() throws IOException {
        String settingsContent = "";
        String buildContent = "";
        Properties propertiesContent = new Properties();
        String rootSettingsContent = null;
        Properties rootPropertiesContent = null;
        if (hasProjectFile(SETTINGS_GRADLE_PATH)) {
            final byte[] settings = readProjectFile(SETTINGS_GRADLE_PATH);
            settingsContent = new String(settings, StandardCharsets.UTF_8);
        }
        if (hasRootProjectFile(SETTINGS_GRADLE_PATH)) {
            final byte[] settings = readRootProjectFile(SETTINGS_GRADLE_PATH);
            rootSettingsContent = new String(settings, StandardCharsets.UTF_8);
        }
        if (hasProjectFile(BUILD_GRADLE_PATH)) {
            final byte[] build = readProjectFile(BUILD_GRADLE_PATH);
            buildContent = new String(build, StandardCharsets.UTF_8);
        }
        if (hasProjectFile(GRADLE_PROPERTIES_PATH)) {
            final byte[] properties = readProjectFile(GRADLE_PROPERTIES_PATH);
            propertiesContent.load(new ByteArrayInputStream(properties));
        }
        if (hasRootProjectFile(GRADLE_PROPERTIES_PATH)) {
            final byte[] properties = readRootProjectFile(GRADLE_PROPERTIES_PATH);
            rootPropertiesContent = new Properties();
            rootPropertiesContent.load(new ByteArrayInputStream(properties));
        }
        return new Model(settingsContent, buildContent, propertiesContent, rootSettingsContent, rootPropertiesContent);
    }

    protected String getBuildContent() throws IOException {
        return getModel().getBuildContent();
    }

    static class Model {
        private String settingsContent;
        private String buildContent;
        private Properties propertiesContent;

        private String rootSettingsContent;
        private Properties rootPropertiesContent;

        public Model(String settingsContent, String buildContent, Properties propertiesContent, String rootSettingsContent,
                Properties rootPropertiesContent) {
            this.settingsContent = settingsContent;
            this.buildContent = buildContent;
            this.propertiesContent = propertiesContent;
            this.rootSettingsContent = rootSettingsContent;
            this.rootPropertiesContent = rootPropertiesContent;
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

        public String getRootSettingsContent() {
            return rootSettingsContent;
        }

        public Properties getRootPropertiesContent() {
            return rootPropertiesContent;
        }

        public void setSettingsContent(String settingsContent) {
            this.settingsContent = settingsContent;
        }

        public void setBuildContent(String buildContent) {
            this.buildContent = buildContent;
        }

    }

}
