package io.quarkus.devtools.commands;

import static io.quarkus.devtools.codestarts.extension.QuarkusExtensionCodestartCatalog.QuarkusExtensionData.*;
import static io.quarkus.devtools.commands.handlers.CreateExtensionCommandHandler.readPom;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import io.quarkus.devtools.codestarts.extension.QuarkusExtensionCodestartCatalog;
import io.quarkus.devtools.codestarts.extension.QuarkusExtensionCodestartCatalog.QuarkusExtensionData;
import io.quarkus.devtools.codestarts.extension.QuarkusExtensionCodestartProjectInput;
import io.quarkus.devtools.codestarts.extension.QuarkusExtensionCodestartProjectInputBuilder;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.commands.handlers.CreateExtensionCommandHandler;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.utilities.MojoUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;
import javax.lang.model.SourceVersion;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;

/**
 * Instances of this class are not thread-safe. They are created per invocation.
 */
public class CreateExtension {

    public enum LayoutType {
        OTHER_PLATFORM,
        QUARKUS_CORE,
        QUARKIVERSE,
        STANDALONE
    }

    public static final String DEFAULT_BOM_GROUP_ID = "io.quarkus";
    public static final String DEFAULT_BOM_ARTIFACT_ID = "quarkus-bom";
    public static final String DEFAULT_BOM_VERSION = "${quarkus.version}";
    public static final String DEFAULT_VERSION = "1.0.0-SNAPSHOT";

    public static final String DEFAULT_CORE_NAMESPACE_ID = "quarkus-";

    public static final String DEFAULT_EXTERNAL_NAMESPACE_ID = "";

    public static final String DEFAULT_QUARKIVERSE_PARENT_GROUP_ID = "io.quarkiverse";
    public static final String DEFAULT_QUARKIVERSE_PARENT_ARTIFACT_ID = "quarkiverse-parent";
    public static final String DEFAULT_QUARKIVERSE_PARENT_VERSION = "7";
    public static final String DEFAULT_QUARKIVERSE_NAMESPACE_ID = "quarkus-";

    private static final String DEFAULT_SUREFIRE_PLUGIN_VERSION = "3.0.0-M5";
    private static final String DEFAULT_COMPILER_PLUGIN_VERSION = "3.8.1";

    private final QuarkusExtensionCodestartProjectInputBuilder builder = QuarkusExtensionCodestartProjectInput.builder();
    private final Path baseDir;

    private final EnhancedDataMap data = new EnhancedDataMap();

    private MessageWriter log = MessageWriter.info();
    private String extensionId;
    private String itTestRelativeDir = "integration-tests";
    private String bomRelativeDir = "bom/application";
    private String extensionsRelativeDir = "extensions";

    public CreateExtension(final Path baseDir) {
        this.baseDir = requireNonNull(baseDir, "extensionDirPath is required");
    }

    public CreateExtension groupId(String groupId) {
        data.putIfNonEmptyString(GROUP_ID, groupId);
        return this;
    }

    public CreateExtension extensionId(String extensionId) {
        if (!StringUtils.isEmpty(extensionId)) {
            this.extensionId = extensionId;
        }
        return this;
    }

    public CreateExtension extensionName(String name) {
        data.putIfNonEmptyString(EXTENSION_NAME, name);
        return this;
    }

    public CreateExtension version(String version) {
        data.putIfNonEmptyString(VERSION, version);
        return this;
    }

    public CreateExtension packageName(String packageName) {
        data.putIfNonEmptyString(PACKAGE_NAME, packageName);
        return this;
    }

    public CreateExtension classNameBase(String classNameBase) {
        data.putIfNonEmptyString(CLASS_NAME_BASE, classNameBase);
        return this;
    }

    public CreateExtension namespaceId(String extensionArtifactIdPrefix) {
        data.putIfNonNull(NAMESPACE_ID, extensionArtifactIdPrefix);
        return this;
    }

    public CreateExtension namespaceName(String namespaceName) {
        data.putIfNonNull(NAMESPACE_NAME, namespaceName);
        return this;
    }

    public CreateExtension parentGroupId(String groupId) {
        data.putIfNonEmptyString(PARENT_GROUP_ID, groupId);
        return this;
    }

    public CreateExtension parentArtifactId(String artifactId) {
        data.putIfNonEmptyString(PARENT_ARTIFACT_ID, artifactId);
        return this;
    }

    public CreateExtension parentVersion(String version) {
        data.putIfNonEmptyString(PARENT_VERSION, version);
        return this;
    }

    public CreateExtension parentRelativePath(String parentRelativePath) {
        data.putIfNonEmptyString(PARENT_RELATIVE_PATH, parentRelativePath);
        return this;
    }

    public CreateExtension quarkusVersion(String quarkusVersion) {
        data.putIfNonEmptyString(QUARKUS_VERSION, quarkusVersion);
        return this;
    }

    public CreateExtension quarkusBomGroupId(String quarkusBomGroupId) {
        data.putIfNonEmptyString(QUARKUS_BOM_GROUP_ID, quarkusBomGroupId);
        return this;
    }

    public CreateExtension quarkusBomArtifactId(String quarkusBomArtifactId) {
        data.putIfNonEmptyString(QUARKUS_BOM_ARTIFACT_ID, quarkusBomArtifactId);
        return this;
    }

    public CreateExtension quarkusBomVersion(String quarkusBomVersion) {
        data.putIfNonEmptyString(QUARKUS_BOM_VERSION, quarkusBomVersion);
        return this;
    }

    public CreateExtension withoutUnitTest(boolean withoutUnitTest) {
        this.builder.withoutUnitTest(withoutUnitTest);
        return this;
    }

    public CreateExtension withoutDevModeTest(boolean withoutDevModeTest) {
        this.builder.withoutDevModeTest(withoutDevModeTest);
        return this;
    }

    public CreateExtension withoutIntegrationTests(boolean withoutIntegrationTest) {
        this.builder.withoutIntegrationTests(withoutIntegrationTest);
        return this;
    }

    public CreateExtension itTestRelativeDir(String itTestRelativeDir) {
        if (!isEmpty(itTestRelativeDir)) {
            this.itTestRelativeDir = itTestRelativeDir;
        }
        return this;
    }

    public CreateExtension bomRelativeDir(String bomRelativeDir) {
        if (!isEmpty(bomRelativeDir)) {
            this.bomRelativeDir = bomRelativeDir;
        }
        return this;
    }

    public CreateExtension extensionsRelativeDir(String extensionsRelativeDir) {
        if (!isEmpty(extensionsRelativeDir)) {
            this.extensionsRelativeDir = extensionsRelativeDir;
        }
        return this;
    }

    public CreateExtension messageWriter(MessageWriter log) {
        this.log = log;
        return this;
    }

    public CreateExtensionCommandHandler prepare() throws QuarkusCommandException {
        final Path workingDir = resolveWorkingDir(baseDir);
        final Model baseModel = resolveModel(baseDir);
        final LayoutType layoutType = detectLayoutType(baseModel, data.getStringValue(GROUP_ID).orElse(null));

        data.putIfAbsent(NAMESPACE_ID, getDefaultNamespaceId(layoutType));
        ensureRequiredStringData(EXTENSION_ID, resolveExtensionId());
        data.putIfAbsent(EXTENSION_NAME, toCapWords(extensionId));
        data.putIfAbsent(NAMESPACE_NAME, computeDefaultNamespaceName(data.getRequiredStringValue(NAMESPACE_ID)));
        data.putIfAbsent(CLASS_NAME_BASE, toCapCamelCase(extensionId));

        final String runtimeArtifactId = getRuntimeArtifactIdFromData();

        ensureRequiredStringData(GROUP_ID, resolveGroupId(baseModel));
        ensureRequiredStringData(VERSION, resolveVersion(baseModel));
        ensureRequiredStringData(PACKAGE_NAME,
                resolveExtensionPackage(data.getRequiredStringValue(GROUP_ID), extensionId));

        final String groupId = data.getRequiredStringValue(GROUP_ID);
        final Model itTestModel;
        String extensionDirName = runtimeArtifactId;
        switch (layoutType) {
            case QUARKUS_CORE:
            case OTHER_PLATFORM:
                extensionDirName = extensionId;
                final Model extensionsParentModel = readPom(workingDir.resolve(extensionsRelativeDir));
                data.putIfAbsent(PROPERTIES_FROM_PARENT, true);
                ensureRequiredStringData(PARENT_GROUP_ID, resolveGroupId(extensionsParentModel));
                ensureRequiredStringData(PARENT_ARTIFACT_ID, resolveArtifactId(extensionsParentModel));
                ensureRequiredStringData(PARENT_VERSION, resolveVersion(extensionsParentModel));

                data.putIfAbsent(PARENT_RELATIVE_PATH, "../pom.xml");
                itTestModel = readPom(workingDir.resolve(itTestRelativeDir));
                break;
            case QUARKIVERSE:
                data.putIfAbsent(PARENT_GROUP_ID, DEFAULT_QUARKIVERSE_PARENT_GROUP_ID);
                data.putIfAbsent(PARENT_ARTIFACT_ID, DEFAULT_QUARKIVERSE_PARENT_ARTIFACT_ID);
                data.putIfAbsent(PARENT_VERSION, DEFAULT_QUARKIVERSE_PARENT_VERSION);
                data.putIfAbsent(QUARKUS_BOM_GROUP_ID, DEFAULT_BOM_GROUP_ID);
                data.putIfAbsent(QUARKUS_BOM_ARTIFACT_ID, DEFAULT_BOM_ARTIFACT_ID);
                data.putIfAbsent(QUARKUS_BOM_VERSION, DEFAULT_BOM_VERSION);
                data.putIfAbsent(MAVEN_COMPILER_PLUGIN_VERSION, DEFAULT_COMPILER_PLUGIN_VERSION);
                ensureRequiredStringData(QUARKUS_VERSION);
                // TODO: Support Quarkiverse multi extensions repo
                builder.addCodestart(QuarkusExtensionCodestartCatalog.Code.QUARKIVERSE.key());
                builder.addCodestart(QuarkusExtensionCodestartCatalog.Tooling.GIT.key());
                itTestModel = getStandaloneTempModel(workingDir, runtimeArtifactId);
                break;
            default:
                data.putIfAbsent(QUARKUS_BOM_GROUP_ID, DEFAULT_BOM_GROUP_ID);
                data.putIfAbsent(QUARKUS_BOM_ARTIFACT_ID, DEFAULT_BOM_ARTIFACT_ID);
                data.putIfAbsent(QUARKUS_BOM_VERSION, DEFAULT_BOM_VERSION);
                data.putIfAbsent(MAVEN_SUREFIRE_PLUGIN_VERSION, DEFAULT_SUREFIRE_PLUGIN_VERSION);
                data.putIfAbsent(MAVEN_COMPILER_PLUGIN_VERSION, DEFAULT_COMPILER_PLUGIN_VERSION);
                ensureRequiredStringData(QUARKUS_VERSION);

                // In standalone mode, the base pom is used as parent for integration tests
                itTestModel = getStandaloneTempModel(workingDir, runtimeArtifactId);
                break;
        }

        ensureRequiredStringData(IT_PARENT_GROUP_ID, resolveGroupId(itTestModel));
        ensureRequiredStringData(IT_PARENT_ARTIFACT_ID, resolveArtifactId(itTestModel));
        ensureRequiredStringData(IT_PARENT_VERSION, resolveVersion(itTestModel));
        ensureRequiredStringData(IT_PARENT_RELATIVE_PATH, "../pom.xml");

        builder.addData(data);

        log.info("\nDetected layout type is '%s' ", layoutType.toString().toLowerCase());
        log.info("Generated runtime artifactId is '%s'\n", runtimeArtifactId);

        if (LayoutType.QUARKUS_CORE.equals(layoutType) || LayoutType.OTHER_PLATFORM.equals(layoutType)) {
            final Path extensionsDir = workingDir.resolve(extensionsRelativeDir);
            final Path itTestDir = workingDir.resolve(itTestRelativeDir);
            final Path bomDir = workingDir.resolve(bomRelativeDir);
            return new CreateExtensionCommandHandler(groupId, runtimeArtifactId, builder.build(),
                    extensionsDir.resolve(extensionDirName),
                    extensionsDir,
                    itTestDir, bomDir);
        }
        return new CreateExtensionCommandHandler(groupId, runtimeArtifactId, builder.build(),
                workingDir.resolve(extensionDirName));
    }

    public QuarkusCommandOutcome execute() throws QuarkusCommandException {
        return prepare().execute(log);
    }

    private String resolveExtensionId() {
        String namespaceId = data.getRequiredStringValue(NAMESPACE_ID);
        if (extensionId.startsWith(namespaceId)) {
            extensionId = extensionId.substring(namespaceId.length());
        }
        return extensionId;
    }

    private String getDefaultNamespaceId(LayoutType layoutType) {
        switch (layoutType) {
            case QUARKIVERSE:
                return DEFAULT_QUARKIVERSE_NAMESPACE_ID;
            case QUARKUS_CORE:
                return DEFAULT_CORE_NAMESPACE_ID;
            default:
                return DEFAULT_EXTERNAL_NAMESPACE_ID;
        }
    }

    private String computeDefaultNamespaceName(String namespaceId) {
        if (isEmpty(namespaceId)) {
            return "";
        }
        return toCapWords(namespaceId) + " - ";
    }

    public static Model resolveModel(Path dir) throws QuarkusCommandException {
        final Path workingDir = resolveWorkingDir(dir);
        final Path basePom = workingDir.resolve("pom.xml");
        if (!Files.isRegularFile(basePom)) {
            return null;
        }
        try {
            return MojoUtils.readPom(basePom.toFile());
        } catch (IOException e) {
            throw new QuarkusCommandException("Error while reading base pom.xml", e);
        }
    }

    private Model getStandaloneTempModel(Path workingDir, String runtimeArtifactId) {
        final Model model = new Model();
        model.setGroupId(data.getRequiredStringValue(GROUP_ID));
        model.setArtifactId(runtimeArtifactId + "-parent");
        model.setVersion(data.getStringValue(VERSION).orElse(DEFAULT_VERSION));
        model.setPomFile(workingDir.resolve("pom.xml").toFile());
        return model;
    }

    private String getRuntimeArtifactIdFromData() {
        return data.getStringValue(NAMESPACE_ID).orElse("")
                + data.getRequiredStringValue(EXTENSION_ID);
    }

    private static Path resolveWorkingDir(Path dir) {
        return "extensions".equals(dir.getFileName().toString()) ? dir.resolve("..") : dir;
    }

    public static LayoutType detectLayoutType(Model basePom, String groupId) {
        if (basePom != null) {
            if (basePom.getArtifactId().endsWith("quarkus-parent")) {
                return LayoutType.QUARKUS_CORE;
            }
            if (basePom.getModules().stream().anyMatch(s -> s.contains("bom"))) {
                return LayoutType.OTHER_PLATFORM;
            }
        }
        if (isQuarkiverseGroupId(groupId))
            return LayoutType.QUARKIVERSE;
        return LayoutType.STANDALONE;
    }

    public static boolean isQuarkiverseGroupId(String groupId) {
        return groupId != null && groupId.contains(DEFAULT_QUARKIVERSE_PARENT_GROUP_ID);
    }

    public static String extractQuarkiverseExtensionId(String groupId) {
        return groupId.replace(DEFAULT_QUARKIVERSE_PARENT_GROUP_ID + ".", "");
    }

    private void ensureRequiredStringData(QuarkusExtensionData key) throws QuarkusCommandException {
        if (!data.containsNonEmptyStringForKey(key)) {
            throw new QuarkusCommandException("'" + key.toString() + "' value is required.");
        }
    }

    private void ensureRequiredStringData(QuarkusExtensionData key, String detectedValue) throws QuarkusCommandException {
        if (!data.containsNonEmptyStringForKey(key)) {
            if (isEmpty(detectedValue)) {
                throw new QuarkusCommandException(
                        "You need to define  '" + key.toString() + "' because it was not found in the project hierarchy.");
            }
            data.putIfNonEmptyString(key, detectedValue);
        }
    }

    static String resolveExtensionPackage(String groupId, String artifactId) {
        final Stack<String> segments = new Stack<>();
        for (String segment : groupId.split("[.\\-]+")) {
            if (segments.isEmpty() || !segments.peek().equals(segment)) {
                segments.add(segment);
            }
        }
        for (String segment : artifactId.split("[.\\-]+")) {
            if (!segments.contains(segment)) {
                segments.add(segment);
            }
        }
        return segments.stream() //
                .map(s -> s.toLowerCase(Locale.ROOT)) //
                .map(s -> SourceVersion.isKeyword(s) ? s + "_" : s) //
                .collect(Collectors.joining("."));
    }

    static String resolveGroupId(Model basePom) {
        return basePom != null ? basePom.getGroupId() != null ? basePom.getGroupId()
                : basePom.getParent() != null && basePom.getParent().getGroupId() != null
                        ? basePom.getParent().getGroupId()
                        : null
                : null;
    }

    static String resolveArtifactId(Model basePom) {
        return basePom != null ? basePom.getArtifactId() != null ? basePom.getArtifactId()
                : basePom.getParent() != null && basePom.getParent().getArtifactId() != null
                        ? basePom.getParent().getArtifactId()
                        : null
                : null;
    }

    static String resolveVersion(Model basePom) {
        return basePom != null ? basePom.getVersion() != null ? basePom.getVersion()
                : basePom.getParent() != null && basePom.getParent().getVersion() != null
                        ? basePom.getParent().getVersion()
                        : DEFAULT_VERSION
                : DEFAULT_VERSION;
    }

    static String toCapCamelCase(String name) {
        final StringBuilder sb = new StringBuilder(name.length());
        for (String segment : name.split("[.\\-]+")) {
            sb.append(Character.toUpperCase(segment.charAt(0)));
            if (segment.length() > 1) {
                sb.append(segment.substring(1));
            }
        }
        return sb.toString();
    }

    static String toCapWords(String name) {
        final StringBuilder sb = new StringBuilder(name.length());
        for (String segment : name.split("[.\\-]+")) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(segment.charAt(0)));
            if (segment.length() > 1) {
                sb.append(segment.substring(1));
            }
        }
        return sb.toString();
    }

    private static class EnhancedDataMap extends HashMap<String, Object> {

        public Optional<String> getStringValue(QuarkusExtensionData key) {
            final Object o = get(key.key());
            return Optional.ofNullable((o instanceof String) ? (String) o : null);
        }

        public void putIfAbsent(QuarkusExtensionData dataKey, Object value) {
            this.putIfAbsent(dataKey.key(), value);
        }

        public void putIfNonEmptyString(QuarkusExtensionData dataKey, String value) {
            if (!StringUtils.isEmpty(value)) {
                this.put(dataKey.key(), value);
            }
        }

        public void putIfNonNull(QuarkusExtensionData dataKey, String value) {
            if (value != null) {
                this.put(dataKey.key(), value);
            }
        }

        public String getRequiredStringValue(QuarkusExtensionData key) {
            return requireNonNull(getStringValue(key).orElse(null), key.key() + " is required");
        }

        public boolean containsNonEmptyStringForKey(QuarkusExtensionData dataKey) {
            return !StringUtils.isEmpty(getStringValue(dataKey).orElse(null));
        }

        public boolean containsKey(QuarkusExtensionData dataKey) {
            return containsKey(dataKey.key());
        }
    }
}
