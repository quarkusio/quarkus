package io.quarkus.devtools.project.buildfile;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.maven.ArtifactKey;
import io.quarkus.registry.catalog.ExtensionCatalog;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// We keep it here to take advantage of the abstract tests
abstract class AbstractGradleBuildFile extends BuildFile {

    private static final Pattern DEPENDENCIES_SECTION = Pattern.compile("^[\\t ]*dependencies\\s*\\{\\s*$", Pattern.MULTILINE);

    private static final String GRADLE_PROPERTIES_PATH = "gradle.properties";

    private final Path rootProjectPath;

    private final AtomicReference<Model> modelReference = new AtomicReference<>();

    public AbstractGradleBuildFile(final Path projectDirPath, final ExtensionCatalog catalog) {
        this(projectDirPath, catalog, null);
    }

    public AbstractGradleBuildFile(final Path projectDirPath, final ExtensionCatalog catalog,
            Path rootProjectPath) {
        super(projectDirPath, catalog);
        this.rootProjectPath = rootProjectPath;
    }

    abstract String getSettingsGradlePath();

    abstract String getBuildGradlePath();

    @Override
    public void writeToDisk() throws IOException {
        if (rootProjectPath != null) {
            Files.write(rootProjectPath.resolve(getSettingsGradlePath()), getModel().getRootSettingsContent().getBytes());
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                getModel().getRootPropertiesContent().store(out, "Gradle properties");
                Files.write(rootProjectPath.resolve(GRADLE_PROPERTIES_PATH),
                        out.toByteArray());
            }
        } else {
            writeToProjectFile(getSettingsGradlePath(), getModel().getSettingsContent().getBytes());
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                getModel().getPropertiesContent().store(out, "Gradle properties");
                writeToProjectFile(GRADLE_PROPERTIES_PATH, out.toByteArray());
            }
        }
        writeToProjectFile(getBuildGradlePath(), getModel().getBuildContent().getBytes());
    }

    static boolean containsProperty(ArtifactCoords coords) {
        return coords.getGroupId().charAt(0) == '$' || coords.getArtifactId().charAt(0) == '$'
                || coords.getVersion() != null && coords.getVersion().charAt(0) == '$';
    }

    static String createDependencyCoordinatesString(ArtifactCoords coords, boolean managed, char quoteChar) {
        StringBuilder newDependency = new StringBuilder().append(quoteChar)
                .append(coords.getGroupId()).append(":").append(coords.getArtifactId());
        if (!managed &&
                (coords.getVersion() != null && !coords.getVersion().isEmpty())) {
            newDependency.append(":").append(coords.getVersion());
        }
        boolean isBOM = "pom".equals(coords.getType());
        if (isBOM && !managed) {
            return String.format("enforcedPlatform(%s)", newDependency.append(quoteChar).toString());
        }
        return newDependency.append(quoteChar).toString();
    }

    static boolean addDependencyInModel(Model model, String newDependency) {
        StringBuilder buildContent = new StringBuilder(model.getBuildContent());
        // Add dependency after "dependencies {"
        Matcher matcher = DEPENDENCIES_SECTION.matcher(buildContent);
        if (matcher.find()) {
            // The line below fails on Windows if System.lineSeparator() is used
            int nextLine = buildContent.indexOf("\n", matcher.start()) + 1;
            buildContent.insert(nextLine, newDependency);
        } else {
            // if no "dependencies {" found, add one
            buildContent.append("dependencies {").append(System.lineSeparator())
                    .append(newDependency)
                    .append("}").append(System.lineSeparator());
        }
        model.setBuildContent(buildContent.toString());
        return true;
    }

    static String getProperty(Model model, String propertyName) {
        final String property = model.getPropertiesContent().getProperty(propertyName);
        if (property != null || model.getRootPropertiesContent() == null) {
            return property;
        }
        return model.getRootPropertiesContent().getProperty(propertyName);
    }

    @Override
    protected void removeDependency(ArtifactKey key) {
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

    Model getModel() {
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

    private boolean hasRootProjectFile(final String fileName) {
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
        if (hasProjectFile(getSettingsGradlePath())) {
            final byte[] settings = readProjectFile(getSettingsGradlePath());
            settingsContent = new String(settings, StandardCharsets.UTF_8);
        }
        if (hasRootProjectFile(getSettingsGradlePath())) {
            final byte[] settings = readRootProjectFile(getSettingsGradlePath());
            rootSettingsContent = new String(settings, StandardCharsets.UTF_8);
        }
        if (hasProjectFile(getBuildGradlePath())) {
            final byte[] build = readProjectFile(getBuildGradlePath());
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

    protected String getBuildContent() {
        return getModel().getBuildContent();
    }

    static class Model {
        private String settingsContent;
        private String buildContent;
        private final Properties propertiesContent;

        private final String rootSettingsContent;
        private final Properties rootPropertiesContent;

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
