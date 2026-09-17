package io.quarkus.maven;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.JvmOptions;
import io.quarkus.bootstrap.util.PropertyUtils;
import io.quarkus.devtools.project.extensions.ScmInfoProvider;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.maven.capabilities.CapabilitiesConfig;
import io.quarkus.maven.capabilities.CapabilityConfig;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.platform.tools.ExtensionMetadataValidator;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Maven-independent logic for generating Quarkus extension descriptors.
 * <p>
 * Generates {@code META-INF/quarkus-extension.properties} and
 * {@code META-INF/quarkus-extension.yaml} for a Quarkus extension runtime module.
 * <p>
 * This class is decoupled from Maven APIs so it can be invoked by alternative
 * build tools (e.g. qraven) that have their own dependency resolution.
 */
public class ExtensionDescriptorGenerator {

    public interface Logger {
        void debug(String msg);

        void warn(String msg);

        void error(String msg);
    }

    public static class DepNode {
        private final String groupId;
        private final String artifactId;
        private final String classifier;
        private final String extension;
        private final String version;
        private final Path resolvedPath;
        private final List<DepNode> children;

        public DepNode(String groupId, String artifactId, String classifier, String extension,
                String version, Path resolvedPath, List<DepNode> children) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.classifier = classifier != null ? classifier : "";
            this.extension = extension != null ? extension : "jar";
            this.version = version;
            this.resolvedPath = resolvedPath;
            this.children = children != null ? children : List.of();
        }

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getClassifier() {
            return classifier;
        }

        public String getExtension() {
            return extension;
        }

        public String getVersion() {
            return version;
        }

        public Path getResolvedPath() {
            return resolvedPath;
        }

        public List<DepNode> getChildren() {
            return children;
        }

        public ArtifactKey key() {
            return ArtifactKey.of(groupId, artifactId, classifier, extension);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(groupId).append(':').append(artifactId).append(':').append(extension);
            if (classifier != null && !classifier.isEmpty()) {
                sb.append(':').append(classifier);
            }
            sb.append(':').append(version);
            return sb.toString();
        }
    }

    public static class ModelDependency {
        private final String groupId;
        private final String artifactId;
        private final String classifier;
        private final String type;
        private final String version;
        private final String scope;
        private final boolean optional;

        public ModelDependency(String groupId, String artifactId, String classifier,
                String type, String version, String scope, boolean optional) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.classifier = classifier;
            this.type = type;
            this.version = version;
            this.scope = scope;
            this.optional = optional;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getClassifier() {
            return classifier;
        }

        public String getType() {
            return type;
        }

        public String getVersion() {
            return version;
        }

        public String getScope() {
            return scope;
        }

        public boolean isOptional() {
            return optional;
        }
    }

    public static class DevModeConfig {
        private final JvmOptions jvmOptions;
        private final JvmOptions xxJvmOptions;
        private final List<String> lockJvmOptions;
        private final List<String> lockXxJvmOptions;

        public DevModeConfig(JvmOptions jvmOptions, JvmOptions xxJvmOptions,
                List<String> lockJvmOptions, List<String> lockXxJvmOptions) {
            this.jvmOptions = jvmOptions;
            this.xxJvmOptions = xxJvmOptions;
            this.lockJvmOptions = lockJvmOptions != null ? lockJvmOptions : List.of();
            this.lockXxJvmOptions = lockXxJvmOptions != null ? lockXxJvmOptions : List.of();
        }

        public JvmOptions getJvmOptions() {
            return jvmOptions;
        }

        public JvmOptions getXxJvmOptions() {
            return xxJvmOptions;
        }

        public List<String> getLockJvmOptions() {
            return lockJvmOptions;
        }

        public List<String> getLockXxJvmOptions() {
            return lockXxJvmOptions;
        }

        public boolean hasLockedJvmOptions() {
            return !lockJvmOptions.isEmpty();
        }

        public boolean hasLockedXxJvmOptions() {
            return !lockXxJvmOptions.isEmpty();
        }
    }

    public static class RemovedResourceEntry {
        private final String key;
        private final String resources;

        public RemovedResourceEntry(String key, String resources) {
            this.key = key;
            this.resources = resources;
        }

        public String getKey() {
            return key;
        }

        public String getResources() {
            return resources;
        }
    }

    /**
     * Abstracts dependency resolution away from Maven's RepositorySystem.
     */
    public interface DependencyResolver {
        DepNode resolveRuntimeDependencies() throws Exception;

        DepNode collectDeploymentDependencies(ArtifactCoords coords) throws Exception;

        Path resolveArtifact(String groupId, String artifactId,
                String classifier, String type, String version) throws Exception;

        boolean isInWorkspace(String groupId, String artifactId);

        Path workspaceClassesDir(String groupId, String artifactId);

        boolean isParallelBuild();

        boolean isAttachedArtifact(ArtifactCoords coords);
    }

    private static final String GROUP_ID = "group-id";
    private static final String ARTIFACT_ID = "artifact-id";
    private static final String METADATA = "metadata";
    private static final String COMMA = ",";

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String projectName;
    private final String projectDescription;
    private final String deployment;
    private final CapabilitiesConfig capabilities;
    private final List<String> conditionalDependencies;
    private final List<String> conditionalDevDependencies;
    private final List<String> dependencyCondition;
    private final List<String> excludedArtifacts;
    private final List<RemovedResourceEntry> removedResources;
    private final List<String> parentFirstArtifacts;
    private final List<String> runnerParentFirstArtifacts;
    private final List<String> lesserPriorityArtifacts;
    private final DevModeConfig devMode;
    private final String minimumJavaVersion;
    private final String requiresQuarkusCore;
    private final boolean skipExtensionValidation;
    private final boolean ignoreNotDetectedQuarkusCoreVersion;
    private final boolean skipCodestartValidation;
    private final Path outputDirectory;
    private final Path extensionFile;
    private final String scmUrl;
    private final List<ModelDependency> modelDependencies;

    private final DependencyResolver resolver;
    private final Logger logger;

    private ArtifactCoords deploymentCoords;
    private DepNode runtimeDeps;
    private DepNode collectedDeploymentDeps;

    public static class Builder {
        private String groupId;
        private String artifactId;
        private String version;
        private String projectName;
        private String projectDescription;
        private String deployment;
        private CapabilitiesConfig capabilities = new CapabilitiesConfig();
        private List<String> conditionalDependencies = new ArrayList<>(0);
        private List<String> conditionalDevDependencies = new ArrayList<>(0);
        private List<String> dependencyCondition = new ArrayList<>(0);
        private List<String> excludedArtifacts;
        private List<RemovedResourceEntry> removedResources = List.of();
        private List<String> parentFirstArtifacts;
        private List<String> runnerParentFirstArtifacts;
        private List<String> lesserPriorityArtifacts;
        private DevModeConfig devMode;
        private String minimumJavaVersion;
        private String requiresQuarkusCore;
        private boolean skipExtensionValidation;
        private boolean ignoreNotDetectedQuarkusCoreVersion;
        private boolean skipCodestartValidation;
        private Path outputDirectory;
        private Path extensionFile;
        private String scmUrl;
        private List<ModelDependency> modelDependencies = List.of();
        private DependencyResolver resolver;
        private Logger logger;

        public Builder groupId(String v) {
            this.groupId = v;
            return this;
        }

        public Builder artifactId(String v) {
            this.artifactId = v;
            return this;
        }

        public Builder version(String v) {
            this.version = v;
            return this;
        }

        public Builder projectName(String v) {
            this.projectName = v;
            return this;
        }

        public Builder projectDescription(String v) {
            this.projectDescription = v;
            return this;
        }

        public Builder deployment(String v) {
            this.deployment = v;
            return this;
        }

        public Builder capabilities(CapabilitiesConfig v) {
            this.capabilities = v;
            return this;
        }

        public Builder conditionalDependencies(List<String> v) {
            this.conditionalDependencies = v != null ? new ArrayList<>(v) : new ArrayList<>(0);
            return this;
        }

        public Builder conditionalDevDependencies(List<String> v) {
            this.conditionalDevDependencies = v != null ? new ArrayList<>(v) : new ArrayList<>(0);
            return this;
        }

        public Builder dependencyCondition(List<String> v) {
            this.dependencyCondition = v != null ? new ArrayList<>(v) : new ArrayList<>(0);
            return this;
        }

        public Builder excludedArtifacts(List<String> v) {
            this.excludedArtifacts = v;
            return this;
        }

        public Builder removedResources(List<RemovedResourceEntry> v) {
            this.removedResources = v != null ? v : List.of();
            return this;
        }

        public Builder parentFirstArtifacts(List<String> v) {
            this.parentFirstArtifacts = v;
            return this;
        }

        public Builder runnerParentFirstArtifacts(List<String> v) {
            this.runnerParentFirstArtifacts = v;
            return this;
        }

        public Builder lesserPriorityArtifacts(List<String> v) {
            this.lesserPriorityArtifacts = v;
            return this;
        }

        public Builder devMode(DevModeConfig v) {
            this.devMode = v;
            return this;
        }

        public Builder minimumJavaVersion(String v) {
            this.minimumJavaVersion = v;
            return this;
        }

        public Builder requiresQuarkusCore(String v) {
            this.requiresQuarkusCore = v;
            return this;
        }

        public Builder skipExtensionValidation(boolean v) {
            this.skipExtensionValidation = v;
            return this;
        }

        public Builder ignoreNotDetectedQuarkusCoreVersion(boolean v) {
            this.ignoreNotDetectedQuarkusCoreVersion = v;
            return this;
        }

        public Builder skipCodestartValidation(boolean v) {
            this.skipCodestartValidation = v;
            return this;
        }

        public Builder outputDirectory(Path v) {
            this.outputDirectory = v;
            return this;
        }

        public Builder extensionFile(Path v) {
            this.extensionFile = v;
            return this;
        }

        public Builder scmUrl(String v) {
            this.scmUrl = v;
            return this;
        }

        public Builder modelDependencies(List<ModelDependency> v) {
            this.modelDependencies = v != null ? v : List.of();
            return this;
        }

        public Builder resolver(DependencyResolver v) {
            this.resolver = v;
            return this;
        }

        public Builder logger(Logger v) {
            this.logger = v;
            return this;
        }

        public ExtensionDescriptorGenerator build() {
            return new ExtensionDescriptorGenerator(this);
        }
    }

    private ExtensionDescriptorGenerator(Builder b) {
        this.groupId = b.groupId;
        this.artifactId = b.artifactId;
        this.version = b.version;
        this.projectName = b.projectName;
        this.projectDescription = b.projectDescription;
        this.deployment = b.deployment;
        this.capabilities = b.capabilities;
        this.conditionalDependencies = b.conditionalDependencies;
        this.conditionalDevDependencies = b.conditionalDevDependencies;
        this.dependencyCondition = b.dependencyCondition;
        this.excludedArtifacts = b.excludedArtifacts;
        this.removedResources = b.removedResources;
        this.parentFirstArtifacts = b.parentFirstArtifacts;
        this.runnerParentFirstArtifacts = b.runnerParentFirstArtifacts;
        this.lesserPriorityArtifacts = b.lesserPriorityArtifacts;
        this.devMode = b.devMode;
        this.minimumJavaVersion = b.minimumJavaVersion;
        this.requiresQuarkusCore = b.requiresQuarkusCore;
        this.skipExtensionValidation = b.skipExtensionValidation;
        this.ignoreNotDetectedQuarkusCoreVersion = b.ignoreNotDetectedQuarkusCoreVersion;
        this.skipCodestartValidation = b.skipCodestartValidation;
        this.outputDirectory = b.outputDirectory;
        this.extensionFile = b.extensionFile;
        this.scmUrl = b.scmUrl;
        this.modelDependencies = b.modelDependencies;
        this.resolver = b.resolver;
        this.logger = b.logger;
    }

    public void generate() throws Exception {

        if (!skipExtensionValidation) {
            validateExtensionDeps();
        }

        final Properties props = new Properties();
        props.setProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT, deployment);

        recordConditionalDeps(props);
        recordCapabilities(props);
        recordClassLoadingConfig(props);
        recordDevModeConfig(props);

        final String quarkusCoreVersion = findQuarkusCoreVersion();
        final String quarkusCoreVersionRange = requiresQuarkusCore == null
                ? toVersionRange(quarkusCoreVersion)
                : requiresQuarkusCore;
        if (quarkusCoreVersionRange != null) {
            props.put(BootstrapConstants.PROP_REQUIRES_QUARKUS_VERSION, quarkusCoreVersionRange);
        }

        final Path output = outputDirectory.resolve(BootstrapConstants.META_INF);
        try {
            Files.createDirectories(output);
            PropertyUtils.store(props, output.resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME));
        } catch (IOException e) {
            throw new Exception(
                    "Failed to persist extension descriptor " + output.resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME),
                    e);
        }

        // extension YAML/JSON
        Path extensionFile = this.extensionFile;
        if (extensionFile == null) {
            extensionFile = output.resolve(BootstrapConstants.QUARKUS_EXTENSION_FILE_NAME);
        }

        ObjectNode extObject;
        ObjectMapper mapper;

        if (!Files.exists(extensionFile)) {
            // check for fallback .json
            Path jsonFallback = extensionFile.getParent().resolve("quarkus-extension.json");
            if (Files.exists(jsonFallback)) {
                extensionFile = jsonFallback;
            }
        }

        if (Files.exists(extensionFile)) {
            mapper = getMapper(extensionFile.toString().endsWith(".yaml"));
            extObject = readExtensionDescriptorFile(extensionFile, mapper);
        } else {
            mapper = getMapper(true);
            extObject = getMapper(true).createObjectNode();
        }

        transformLegacyToNew(extObject, mapper);
        ensureArtifactCoords(extObject);

        if (extObject.get("name") == null) {
            if (projectName != null) {
                extObject.put("name", projectName);
            } else {
                JsonNode node = extObject.get(ARTIFACT_ID);
                String defaultName = node != null ? node.asString() : artifactId;
                int i = 0;
                if (defaultName.startsWith("quarkus-")) {
                    i = "quarkus-".length();
                }
                final StringBuilder buf = new StringBuilder();
                boolean startWord = true;
                while (i < defaultName.length()) {
                    final char c = defaultName.charAt(i++);
                    if (c == '-') {
                        if (!startWord) {
                            buf.append(' ');
                            startWord = true;
                        }
                    } else if (startWord) {
                        buf.append(Character.toUpperCase(c));
                        startWord = false;
                    } else {
                        buf.append(c);
                    }
                }
                defaultName = buf.toString();
                logger.warn("Extension name has not been provided for " + extObject.get(GROUP_ID).asText("") + ":"
                        + extObject.get(ARTIFACT_ID).asText("") + "! Using '" + defaultName
                        + "' as the default one.");
                extObject.put("name", defaultName);
            }
        }
        if (!extObject.has("description") && projectDescription != null) {
            extObject.put("description", projectDescription);
        }

        setBuiltWithQuarkusCoreVersion(quarkusCoreVersion, extObject);
        setRequiresQuarkusCoreVersion(quarkusCoreVersionRange, extObject);
        addJavaVersion(extObject);
        addCapabilities(extObject);
        addSource(extObject);
        addExtensionDependencies(extObject);

        completeCodestartArtifact(mapper, extObject);

        try {
            ExtensionMetadataValidator.validate(extObject);
        } catch (IOException e) {
            throw new Exception(e.getMessage(), e.getCause());
        }

        final DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

        try (BufferedWriter bw = Files
                .newBufferedWriter(output.resolve(BootstrapConstants.QUARKUS_EXTENSION_FILE_NAME))) {
            bw.write(getMapper(true).writer().with(prettyPrinter).writeValueAsString(extObject));
        } catch (IOException e) {
            throw new Exception(
                    "Failed to persist " + output.resolve(BootstrapConstants.QUARKUS_EXTENSION_FILE_NAME), e);
        }

        try (BufferedWriter bw = Files
                .newBufferedWriter(output.resolve(BootstrapConstants.QUARKUS_EXTENSION_JSON_FILE_NAME))) {
            bw.write(getMapper(false).writer().with(prettyPrinter).writeValueAsString(extObject));
        } catch (IOException e) {
            throw new Exception(
                    "Failed to persist " + output.resolve(BootstrapConstants.QUARKUS_EXTENSION_JSON_FILE_NAME), e);
        }
    }

    private void recordDevModeConfig(Properties props) {
        if (devMode == null) {
            return;
        }
        var jvmArgs = devMode.getJvmOptions();
        if (jvmArgs != null && !jvmArgs.isEmpty()) {
            jvmArgs.setAsExtensionDevModeProperties(props);
        }
        jvmArgs = devMode.getXxJvmOptions();
        if (jvmArgs != null && !jvmArgs.isEmpty()) {
            jvmArgs.setAsExtensionDevModeProperties(props);
        }
        if (devMode.hasLockedXxJvmOptions()) {
            props.setProperty(BootstrapConstants.EXT_DEV_MODE_LOCK_XX_JVM_OPTIONS,
                    String.join(COMMA, devMode.getLockXxJvmOptions()));
        }
        if (devMode.hasLockedJvmOptions()) {
            props.setProperty(BootstrapConstants.EXT_DEV_MODE_LOCK_JVM_OPTIONS,
                    String.join(COMMA, devMode.getLockJvmOptions()));
        }
    }

    private void recordClassLoadingConfig(Properties props) throws Exception {
        if (parentFirstArtifacts != null && !parentFirstArtifacts.isEmpty()) {
            String val = String.join(COMMA, parentFirstArtifacts);
            props.put(ApplicationModelBuilder.PARENT_FIRST_ARTIFACTS, val);
        }

        if (runnerParentFirstArtifacts != null && !runnerParentFirstArtifacts.isEmpty()) {
            String val = String.join(COMMA, runnerParentFirstArtifacts);
            props.put(ApplicationModelBuilder.RUNNER_PARENT_FIRST_ARTIFACTS, val);
        }

        if (excludedArtifacts != null && !excludedArtifacts.isEmpty()) {
            String val = String.join(COMMA, excludedArtifacts);
            props.put(ApplicationModelBuilder.EXCLUDED_ARTIFACTS, val);
        }

        if (!removedResources.isEmpty()) {
            for (RemovedResourceEntry entry : removedResources) {
                final ArtifactKey key;
                try {
                    key = ArtifactKey.fromString(entry.key);
                } catch (IllegalArgumentException e) {
                    throw new Exception(
                            "Failed to parse removed resource '" + entry.key + '=' + entry.resources + "'", e);
                }
                if (entry.resources == null || entry.resources.isBlank()) {
                    continue;
                }
                final String[] resources = entry.resources.split(COMMA);
                if (resources.length == 0) {
                    continue;
                }
                final String value;
                if (resources.length == 1) {
                    value = resources[0];
                } else {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(resources[0]);
                    for (int i = 1; i < resources.length; ++i) {
                        final String resource = resources[i];
                        if (!resource.isBlank()) {
                            sb.append(',').append(resource);
                        }
                    }
                    value = sb.toString();
                }
                props.setProperty(ApplicationModelBuilder.REMOVED_RESOURCES_DOT + key, value);
            }
        }

        if (lesserPriorityArtifacts != null && !lesserPriorityArtifacts.isEmpty()) {
            String val = String.join(COMMA, lesserPriorityArtifacts);
            props.put(ApplicationModelBuilder.LESSER_PRIORITY_ARTIFACTS, val);
        }
    }

    private void recordCapabilities(Properties props) {
        if (!capabilities.getProvides().isEmpty()) {
            final StringBuilder buf = new StringBuilder();
            final Iterator<CapabilityConfig> i = capabilities.getProvides().iterator();
            appendCapability(i.next(), buf);
            while (i.hasNext()) {
                appendCapability(i.next(), buf.append(','));
            }
            props.setProperty(BootstrapConstants.PROP_PROVIDES_CAPABILITIES, buf.toString());
        }
        if (!capabilities.getRequires().isEmpty()) {
            final StringBuilder buf = new StringBuilder();
            final Iterator<CapabilityConfig> i = capabilities.getRequires().iterator();
            appendCapability(i.next(), buf);
            while (i.hasNext()) {
                appendCapability(i.next(), buf.append(','));
            }
            props.setProperty(BootstrapConstants.PROP_REQUIRES_CAPABILITIES, buf.toString());
        }
    }

    private void recordConditionalDeps(Properties props) throws Exception {
        lookForConditionalDeps();
        setConditionalDepsProperty(props, BootstrapConstants.CONDITIONAL_DEPENDENCIES, conditionalDependencies);
        setConditionalDepsProperty(props, BootstrapConstants.CONDITIONAL_DEV_DEPENDENCIES, conditionalDevDependencies);
        if (!dependencyCondition.isEmpty()) {
            final StringBuilder buf = new StringBuilder();
            int i = 0;
            buf.append(ArtifactKey.fromString(dependencyCondition.get(i++)).toGacString());
            while (i < dependencyCondition.size()) {
                buf.append(' ').append(ArtifactKey.fromString(dependencyCondition.get(i++)).toGacString());
            }
            props.setProperty(BootstrapConstants.DEPENDENCY_CONDITION, buf.toString());
        }
    }

    private static void setConditionalDepsProperty(Properties props, String propertyName, List<String> list) {
        if (list.isEmpty()) {
            return;
        }
        final StringBuilder buf = new StringBuilder();
        int i = 0;
        buf.append(ArtifactCoords.fromString(list.get(i++)));
        while (i < list.size()) {
            buf.append(' ').append(ArtifactCoords.fromString(list.get(i++)));
        }
        props.setProperty(propertyName, buf.toString());
    }

    private void lookForConditionalDeps() throws Exception {
        if (!conditionalDependencies.isEmpty()) {
            return;
        }
        // if conditional dependencies haven't been configured
        // we check whether there are direct optional dependencies on extensions
        // that are configured with a dependency condition
        // such dependencies will be registered as conditional
        StringBuilder buf = null;
        for (ModelDependency d : modelDependencies) {
            if (!d.isOptional()) {
                continue;
            }
            if (d.getScope() != null && !d.getScope().isEmpty()
                    && !("compile".equals(d.getScope()) || "runtime".equals(d.getScope()))) {
                continue;
            }
            final Properties extProps = getExtensionDescriptor(
                    d.getGroupId(), d.getArtifactId(), d.getClassifier(), d.getType(), d.getVersion());
            if (extProps == null || !extProps.containsKey(BootstrapConstants.DEPENDENCY_CONDITION)) {
                continue;
            }
            if (buf == null) {
                buf = new StringBuilder();
            } else {
                buf.setLength(0);
            }
            buf.append(d.getGroupId()).append(':').append(d.getArtifactId()).append(':');
            if (d.getClassifier() != null) {
                buf.append(d.getClassifier());
            }
            buf.append(':').append(d.getType()).append(':').append(d.getVersion());
            conditionalDependencies.add(buf.toString());
        }
    }

    private void setRequiresQuarkusCoreVersion(String compatibilityRange, ObjectNode extObject) {
        ObjectNode metadata = getMetadataNode(extObject);
        if (!metadata.has("requires-quarkus-core") && compatibilityRange != null) {
            metadata.put("requires-quarkus-core", compatibilityRange);
        }
    }

    static String toVersionRange(String version) {
        if (version == null) {
            return null;
        }

        // we don't use DefaultArtifactVersion here as it doesn't support 4 dotted number parts
        // we might get rid of this version scheme but let's make sure we support it just in case
        String[] versionItems = version.split("-");
        versionItems = versionItems[0].split("\\.");
        return "[" + versionItems[0] + "." + (versionItems.length > 1 ? versionItems[1] : "0") + ",)";
    }

    private void ensureArtifactCoords(ObjectNode extObject) {
        String groupId = null;
        String artifactId = null;
        String version = null;
        final JsonNode artifactNode = extObject.get("artifact");
        if (artifactNode == null) {
            groupId = getRealValueOrNull(extObject.has("groupId") ? extObject.get("groupId").asText() : null,
                    "${project.groupId");
            artifactId = getRealValueOrNull(extObject.has("artifactId") ? extObject.get("artifactId").asText() : null,
                    "${project.artifactId");
            version = getRealValueOrNull(extObject.has("version") ? extObject.get("version").asText() : null,
                    "${project.version");
        } else {
            final String[] coordsArr = artifactNode.asText().split(":");
            if (coordsArr.length > 0) {
                groupId = getRealValueOrNull(coordsArr[0], "${project.groupId}");
                if (coordsArr.length > 1) {
                    artifactId = getRealValueOrNull(coordsArr[1], "${project.artifactId}");
                    if (coordsArr.length > 2) {
                        version = getRealValueOrNull(coordsArr[2], "${project.version}");
                    }
                }
            }
        }
        if (artifactNode == null || groupId == null || artifactId == null || version == null) {
            final ArtifactCoords coords = ArtifactCoords.jar(
                    groupId == null ? this.groupId : groupId,
                    artifactId == null ? this.artifactId : artifactId,
                    version == null ? this.version : version);
            extObject.put("artifact", coords.toString());
            if (!extObject.has(GROUP_ID)) {
                extObject.put(GROUP_ID, groupId == null ? this.groupId : groupId);
            }
            if (!extObject.has(ARTIFACT_ID)) {
                extObject.put(ARTIFACT_ID, artifactId == null ? this.artifactId : artifactId);
            }
        }
    }

    private static String getRealValueOrNull(String s, String propertyExpr) {
        return s != null && !s.isBlank() && !s.equals(propertyExpr) ? s : null;
    }

    private ObjectNode readExtensionDescriptorFile(Path extensionFile, ObjectMapper mapper) throws Exception {
        try (InputStream is = Files.newInputStream(extensionFile)) {
            return mapper.readValue(is, ObjectNode.class);
        } catch (IOException io) {
            throw new Exception("Failed to parse " + extensionFile, io);
        }
    }

    private void completeCodestartArtifact(ObjectMapper mapper, ObjectNode extObject) throws Exception {
        JsonNode mvalue = getJsonElement(extObject, METADATA, "codestart");
        if (mvalue == null || !mvalue.isObject()) {
            return;
        }
        final ObjectNode codestartObject = (ObjectNode) mvalue;
        mvalue = mvalue.get("artifact");
        if (mvalue == null) {
            if (!skipCodestartValidation) {
                throw new Exception("Codestart artifact is missing from the " + extensionFile);
            }
            return;
        }

        String codestartArtifact = getCodestartArtifact(mvalue.asText(), version);
        final ArtifactCoords codestartArtifactCoords = GACTV.fromString(codestartArtifact);
        codestartObject.put("artifact", codestartArtifactCoords.toString());
        if (!skipCodestartValidation) {
            // first we look for it in the workspace, if it's in there we don't need to actually resolve the artifact, because it might not have been built yet
            if (resolver.isInWorkspace(codestartArtifactCoords.getGroupId(),
                    codestartArtifactCoords.getArtifactId())) {
                return;
            }
            if (resolver.isAttachedArtifact(codestartArtifactCoords)) {
                return;
            }
            try {
                resolver.resolveArtifact(codestartArtifactCoords.getGroupId(),
                        codestartArtifactCoords.getArtifactId(),
                        codestartArtifactCoords.getClassifier(),
                        codestartArtifactCoords.getType(),
                        codestartArtifactCoords.getVersion());
            } catch (Exception e) {
                throw new Exception("Failed to resolve codestart artifact " + codestartArtifactCoords, e);
            }
        }
    }

    /**
     * If artifact contains "G:A" the project version is added to have "G:A:V" <br>
     * else the version must be defined either with ${project.version} or hardcoded <br>
     * to be compatible with AppArtifactCoords.fromString
     *
     * @param originalArtifact
     * @param projectVersion
     * @return
     */
    static String getCodestartArtifact(String originalArtifact, String projectVersion) {
        if (originalArtifact.matches("^[^:]+:[^:]+$")) {
            return originalArtifact + ":" + projectVersion;
        }
        return originalArtifact.replace("${project.version}", projectVersion);
    }

    private static JsonNode getJsonElement(ObjectNode extObject, String... elements) {
        JsonNode mvalue = extObject.get(elements[0]);
        int i = 1;
        while (i < elements.length) {
            if (mvalue == null || !mvalue.isObject()) {
                return null;
            }
            final String element = elements[i++];
            extObject = (ObjectNode) mvalue;
            mvalue = extObject.get(element);
        }
        return mvalue;
    }

    private static void appendCapability(CapabilityConfig capability, StringBuilder buf) {
        buf.append(capability.getName());
        if (!capability.getOnlyIf().isEmpty()) {
            for (String onlyIf : capability.getOnlyIf()) {
                buf.append('?').append(onlyIf);
            }
        }
        if (!capability.getOnlyIfNot().isEmpty()) {
            for (String onlyIfNot : capability.getOnlyIfNot()) {
                buf.append("?!").append(onlyIfNot);
            }
        }
    }

    private void setBuiltWithQuarkusCoreVersion(String coreVersion, ObjectNode extObject) throws Exception {
        if (coreVersion != null) {
            ObjectNode metadata = getMetadataNode(extObject);
            metadata.put("built-with-quarkus-core", coreVersion);
        } else if (!ignoreNotDetectedQuarkusCoreVersion) {
            throw new Exception("Failed to determine the Quarkus core version used to build the extension");
        }
    }

    private String findQuarkusCoreVersion() throws Exception {
        final DepNode root;
        try {
            root = resolvedRuntimeDeps();
        } catch (Exception e) {
            throw new Exception("Failed to collect runtime dependencies of "
                    + groupId + ":" + artifactId + ":" + version, e);
        }
        String[] coreVersion = new String[1];
        findQuarkusCore(root, coreVersion);
        return coreVersion[0];
    }

    private void addExtensionDependencies(ObjectNode extObject) throws Exception {
        final DepNode root = resolvedRuntimeDeps();
        ArrayNode[] extensionDeps = new ArrayNode[1];
        walkForExtensionDeps(root, extObject, extensionDeps);
    }

    private void walkForExtensionDeps(DepNode node, ObjectNode extObject, ArrayNode[] extensionDeps) {
        if (node.getResolvedPath() != null && "jar".equals(node.getExtension())) {
            Path p = node.getResolvedPath();
            boolean isExtension = false;
            if (Files.isDirectory(p)) {
                isExtension = getExtensionDescriptorOrNull(p) != null;
            } else {
                if (!Files.exists(p)) {
                    logger.warn("Failed to resolve " + node + ", " + p + " does not exist");
                } else {
                    try (FileSystem fs = ZipUtils.newFileSystem(p)) {
                        isExtension = getExtensionDescriptorOrNull(fs.getPath("")) != null;
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read " + p, e);
                    }
                }
            }
            if (isExtension) {
                if (extensionDeps[0] == null) {
                    extensionDeps[0] = getMetadataNode(extObject).putArray("extension-dependencies");
                }
                extensionDeps[0].add(node.key().toGacString());
            }
        }
        for (DepNode child : node.getChildren()) {
            walkForExtensionDeps(child, extObject, extensionDeps);
        }
    }

    private static boolean findQuarkusCore(DepNode node, String[] result) {
        if ("quarkus-core".equals(node.getArtifactId())) {
            result[0] = node.getVersion();
            if ("io.quarkus".equals(node.getGroupId())) {
                return true;
            }
        }
        for (DepNode child : node.getChildren()) {
            if (findQuarkusCore(child, result)) {
                return true;
            }
        }
        return false;
    }

    private void addSource(ObjectNode extObject) {
        ScmInfoProvider scmInfoProvider = new ScmInfoProvider(scmUrl);
        Map<String, String> repo = scmInfoProvider.getSourceRepo();
        ObjectNode metadata = getMetadataNode(extObject);

        if (repo != null) {
            for (Map.Entry<String, String> e : repo.entrySet()) {
                // Ignore if already set
                String value = e.getValue();
                String fieldName = "scm-" + e.getKey();
                if (!metadata.has(fieldName) && value != null) {
                    // Tools may not be able to handle nesting in metadata, so do fake-nesting
                    metadata.put(fieldName, value);
                }
            }
            String warning = scmInfoProvider.getInconsistencyWarning();
            if (warning != null) {
                logger.warn(warning);
            }
        } else if (!metadata.has("scm-url")) {
            logger.debug(
                    "Could not work out a source control repository from the build environment or build file. Consider adding an scm-url entry in quarkus-extension.yaml");
        }
    }

    public void addJavaVersion(ObjectNode extObject) {
        ObjectNode metadataNode = getMetadataNode(extObject);
        // Ignore if already set
        if (!metadataNode.has("minimum-java-version") && minimumJavaVersion != null) {
            metadataNode.put("minimum-java-version", minimumJavaVersion);
        }
    }

    private void addCapabilities(ObjectNode extObject) {
        ObjectNode capsNode = null;
        if (!capabilities.getProvides().isEmpty()) {
            capsNode = getMetadataNode(extObject).putObject("capabilities");
            final ArrayNode provides = capsNode.putArray("provides");
            for (CapabilityConfig cap : capabilities.getProvides()) {
                provides.add(cap.getName());
            }
        }
        if (!capabilities.getRequires().isEmpty()) {
            if (capsNode == null) {
                capsNode = getMetadataNode(extObject).putObject("capabilities");
            }
            final ArrayNode requires = capsNode.putArray("requires");
            for (CapabilityConfig cap : capabilities.getRequires()) {
                requires.add(cap.getName());
            }
        }
    }

    static ObjectNode getMetadataNode(ObjectNode extObject) {
        JsonNode mvalue = extObject.get(METADATA);
        ObjectNode metadata;
        if (mvalue != null && mvalue.isObject()) {
            metadata = (ObjectNode) mvalue;
        } else {
            metadata = extObject.putObject(METADATA);
        }
        return metadata;
    }

    private void validateExtensionDeps() throws Exception {
        final ArtifactKey rootDeploymentGact = getDeploymentCoords().getKey();
        final RootNode rootDeployment = new RootNode(rootDeploymentGact, 2);
        final Node rootRuntime = rootDeployment.newChild(
                ArtifactKey.of(groupId, artifactId, "", "jar"), 1);

        rootDeployment.expectedDeploymentNodes.put(rootDeployment.gact, rootDeployment);
        rootDeployment.expectedDeploymentNodes.put(rootRuntime.gact, rootRuntime);
        // collect transitive extension deps
        final DepNode resolvedDeps = resolvedRuntimeDeps();
        for (DepNode node : resolvedDeps.getChildren()) {
            rootDeployment.directRuntimeDeps.add(node.key());
        }
        visitRuntimeDeps(rootDeployment, rootDeployment, rootDeployment.id, resolvedDeps);

        final DepNode deploymentNode = collectedDeploymentDeps();
        visitDeploymentDeps(rootDeployment, deploymentNode);

        if (rootDeployment.hasErrors()) {
            logger.error("Quarkus Extension Dependency Verification Error");

            final StringBuilder buf = new StringBuilder();

            if (rootDeployment.deploymentDepsTotal != 0) {
                logger.error("Deployment artifact " + getDeploymentCoords() +
                        " was found to be missing dependencies on the Quarkus extension artifacts marked with '-' below:");
                final List<ArtifactKey> missing = rootDeployment.collectMissingDeploymentDeps(logger);
                buf.append("Deployment artifact ");
                buf.append(getDeploymentCoords());
                buf.append(" is missing the following dependencies from its configuration: ");
                final Iterator<ArtifactKey> i = missing.iterator();
                buf.append(i.next());
                while (i.hasNext()) {
                    buf.append(", ").append(i.next());
                }
            }

            if (!rootDeployment.deploymentsOnRtCp.isEmpty()) {
                if (rootDeployment.runtimeCp > 0) {
                    logger.error("The following deployment artifact(s) appear on the runtime classpath: ");
                    rootDeployment.collectDeploymentsOnRtCp(logger);
                }
                if (buf.length() > 0) {
                    buf.append(System.lineSeparator());
                }
                buf.append("The following deployment artifact(s) appear on the runtime classpath: ");
                final Iterator<ArtifactKey> i = rootDeployment.deploymentsOnRtCp.iterator();
                buf.append(i.next());
                while (i.hasNext()) {
                    buf.append(", ").append(i.next());
                }
            }

            if (!rootDeployment.unexpectedDeploymentDeps.isEmpty()) {
                final List<ArtifactKey> unexpectedRtDeps = new ArrayList<>(0);
                final List<ArtifactKey> unexpectedDeploymentDeps = new ArrayList<>(0);
                for (Map.Entry<ArtifactKey, ArtifactKey> e : rootDeployment.unexpectedDeploymentDeps.entrySet()) {
                    if (rootDeployment.allDeploymentDeps.contains(e.getKey())) {
                        unexpectedDeploymentDeps.add(e.getKey());
                    } else {
                        unexpectedRtDeps.add(e.getValue());
                    }
                }

                final String projectArtifact = groupId + ":" + artifactId;

                if (!unexpectedRtDeps.isEmpty()) {
                    if (buf.length() > 0) {
                        buf.append(System.lineSeparator());
                    }
                    buf.append("The deployment artifact ").append(rootDeploymentGact).append(
                            " depends on the following Quarkus extension runtime artifacts that weren't found among the dependencies of ")
                            .append(projectArtifact).append(":");
                    for (ArtifactKey a : unexpectedRtDeps) {
                        buf.append(' ').append(a);
                    }
                    logger.error("The deployment artifact " + rootDeploymentGact
                            + " depends on the following Quarkus extension runtime artifacts that weren't found among the dependencies of "
                            + projectArtifact + ":");
                    highlightInTree(deploymentNode, unexpectedRtDeps);
                }

                if (!unexpectedDeploymentDeps.isEmpty()) {
                    if (buf.length() > 0) {
                        buf.append(System.lineSeparator());
                    }
                    buf.append("The deployment artifact ").append(rootDeploymentGact).append(
                            " depends on the following Quarkus extension deployment artifacts whose corresponding runtime artifacts were not found among the dependencies of ")
                            .append(projectArtifact).append(":");
                    for (ArtifactKey a : unexpectedDeploymentDeps) {
                        buf.append(' ').append(a);
                    }
                    logger.error("The deployment artifact " + rootDeploymentGact
                            + " depends on the following Quarkus extension deployment artifacts whose corresponding runtime artifacts were not found among the dependencies of "
                            + projectArtifact + ":");
                    highlightInTree(deploymentNode, unexpectedDeploymentDeps);
                }
            }

            throw new Exception(buf.toString());
        }
    }

    private DepNode resolvedRuntimeDeps() throws Exception {
        if (runtimeDeps == null) {
            try {
                runtimeDeps = resolver.resolveRuntimeDependencies();
            } catch (Exception e) {
                throw new Exception("Failed to resolve dependencies of "
                        + groupId + ":" + artifactId + ":" + version, e);
            }
        }
        return runtimeDeps;
    }

    private void highlightInTree(DepNode node, Collection<ArtifactKey> keys) {
        highlightInTree(0, node, keys, new HashSet<>(), new StringBuilder(), new ArrayList<>());
    }

    private void highlightInTree(int depth, DepNode node, Collection<ArtifactKey> keysToHighlight,
            Set<ArtifactKey> visited, StringBuilder buf, List<String> branch) {
        final ArtifactKey key = node.key();
        if (!visited.add(key)) {
            return;
        }
        buf.setLength(0);
        final boolean highlighted = keysToHighlight.contains(key);
        if (highlighted) {
            buf.append('*');
        } else {
            buf.append(' ');
        }
        buf.append("  ".repeat(Math.max(0, depth)));
        buf.append(node);
        branch.add(buf.toString());
        if (!highlighted) {
            for (DepNode child : node.getChildren()) {
                highlightInTree(depth + 1, child, keysToHighlight, visited, buf, branch);
            }
        } else {
            for (String line : branch) {
                logger.error(line);
            }
        }
        branch.remove(branch.size() - 1);
    }

    private void visitDeploymentDeps(RootNode rootDeployment, DepNode dep) throws Exception {
        for (DepNode child : dep.getChildren()) {
            visitDeploymentDep(rootDeployment, child);
        }
    }

    private void visitDeploymentDep(RootNode rootDeployment, DepNode dep) throws Exception {
        final ArtifactKey key = dep.key();
        if (!rootDeployment.allDeploymentDeps.add(key)) {
            return;
        }
        final Node node = rootDeployment.expectedDeploymentNodes.get(key);

        if (node != null) {
            if (!node.present) {
                node.present = true;
                --rootDeployment.deploymentDepsTotal;
                if (rootDeployment.allRtDeps.contains(key)) {
                    rootDeployment.deploymentsOnRtCp.add(key);
                }
            }
        } else if (!rootDeployment.allRtDeps.contains(key)) {
            final ArtifactKey deployment = getDeploymentKey(dep);
            if (deployment != null) {
                rootDeployment.unexpectedDeploymentDeps.put(deployment, dep.key());
            }
        }
        visitDeploymentDeps(rootDeployment, dep);
    }

    private void visitRuntimeDep(RootNode root, Node currentNode, int currentId,
            DepNode node) throws Exception {
        root.allRtDeps.add(node.key());
        final ArtifactKey deployment = getDeploymentKey(node);
        if (deployment != null) {
            currentNode = currentNode.newChild(deployment, ++currentId);
            root.expectedDeploymentNodes.put(currentNode.gact, currentNode);
            ++root.deploymentDepsTotal;
            if (root.allRtDeps.contains(deployment)) {
                root.deploymentsOnRtCp.add(deployment);
                if (root.directRuntimeDeps.contains(deployment)) {
                    currentNode.runtimeCp = 2; // actual rt dep
                    Node n = currentNode.parent;
                    while (n != null) {
                        if (n.runtimeCp != 0) {
                            break;
                        } else {
                            n.runtimeCp = 1; // path to the actual rt dep
                        }
                        n = n.parent;
                    }
                }
            }
        }
        visitRuntimeDeps(root, currentNode, currentId, node);
    }

    private void visitRuntimeDeps(RootNode root, Node currentNode, int currentId, DepNode node) throws Exception {
        for (DepNode child : node.getChildren()) {
            visitRuntimeDep(root, currentNode, currentId, child);
        }
    }

    private ArtifactKey getDeploymentKey(DepNode node) throws Exception {
        final ArtifactCoords deployment = getDeploymentArtifact(node);
        return deployment == null ? null : deployment.getKey();
    }

    private ArtifactCoords getDeploymentArtifact(DepNode node) throws Exception {
        final Properties props = getExtensionDescriptor(
                node.getGroupId(), node.getArtifactId(), node.getClassifier(),
                node.getExtension(), node.getVersion());
        if (props == null) {
            return null;
        }
        final String deploymentStr = props.getProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT);
        if (deploymentStr == null) {
            throw new IllegalStateException("Quarkus extension runtime artifact " + node + " is missing "
                    + BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT + " property in its "
                    + BootstrapConstants.DESCRIPTOR_PATH);
        }
        return ArtifactCoords.fromString(deploymentStr);
    }

    private Properties getExtensionDescriptor(String groupId, String artifactId, String classifier, String type,
            String version) {
        // if it hasn't been packaged yet, we skip it, we are not packaging yet
        if (!ArtifactCoords.TYPE_JAR.equals(type)) {
            return null;
        }
        Path f;
        try {
            f = resolver.resolveArtifact(groupId, artifactId, classifier, type, version);
        } catch (Exception e) {
            logger.warn("Failed to resolve " + groupId + ":" + artifactId);
            return null;
        }
        if (f == null) {
            return null;
        }
        try {
            if (Files.isDirectory(f)) {
                return readExtensionDescriptorIfExists(f);
            }
            // In case of a parallel build, the resolved JAR might not have been fully written, which may result in a failure to read it
            // so we try the classes dir first
            if (resolver.isParallelBuild()) {
                Path classesDir = resolver.workspaceClassesDir(groupId, artifactId);
                if (classesDir != null && Files.exists(classesDir)) {
                    return readExtensionDescriptorIfExists(classesDir);
                }
            }
            if (!Files.exists(f)) {
                return null;
            }
            try (FileSystem fs = ZipUtils.newFileSystem(f)) {
                return readExtensionDescriptorIfExists(fs.getPath(""));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read " + f, e);
        }
    }

    private static Properties readExtensionDescriptorIfExists(Path dir) throws IOException {
        final Path p = getExtensionDescriptorOrNull(dir);
        return p == null ? null : readExtensionDescriptor(p);
    }

    private static Path getExtensionDescriptorOrNull(Path runtimeExtRootDir) {
        final Path p = runtimeExtRootDir.resolve(BootstrapConstants.DESCRIPTOR_PATH);
        return Files.exists(p) ? p : null;
    }

    private static Properties readExtensionDescriptor(Path extDescr) throws IOException {
        final Properties props = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(extDescr)) {
            props.load(reader);
        }
        return props;
    }

    private DepNode collectedDeploymentDeps() throws Exception {
        if (collectedDeploymentDeps == null) {
            final ArtifactCoords depCoords = getDeploymentCoords();
            try {
                collectedDeploymentDeps = resolver.collectDeploymentDependencies(depCoords);
            } catch (Exception e) {
                throw new Exception("Failed to collect dependencies of deployment artifact " + depCoords, e);
            }
        }
        return collectedDeploymentDeps;
    }

    private ArtifactCoords getDeploymentCoords() {
        if (deploymentCoords == null) {
            deploymentCoords = ArtifactCoords.fromString(deployment);
        }
        return deploymentCoords;
    }

    private static boolean isJarFile(Path f) {
        return f != null && f.getFileName().toString().endsWith(".jar")
                && Files.exists(f) && !Files.isDirectory(f);
    }

    private static void transformLegacyToNew(ObjectNode extObject, ObjectMapper mapper) {
        ObjectNode metadata = getMetadataNode(extObject);

        // Note: groupId and artifactId shouldn't normally be in the source json but
        // just putting it
        // here for completenes
        if (extObject.get("groupId") != null) {
            extObject.set(GROUP_ID, extObject.get("groupId"));
            extObject.remove("groupId");
        }
        if (extObject.get("artifactId") != null) {
            extObject.set(ARTIFACT_ID, extObject.get("artifactId"));
            extObject.remove("artifactId");
        }
        if (extObject.get("labels") != null) {
            metadata.set("keywords", extObject.get("labels"));
            extObject.remove("labels");
        }
        if (extObject.get("guide") != null) {
            metadata.set("guide", extObject.get("guide"));
            extObject.remove("guide");
        }
        if (extObject.get("shortName") != null) {
            metadata.set("short-name", extObject.get("shortName"));
            extObject.remove("shortName");
        }
        extObject.set(METADATA, metadata);
    }

    private ObjectMapper getMapper(boolean yaml) {
        if (yaml) {
            return YAMLMapper.builder()
                    .propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
                    .build();
        } else {
            return JsonMapper.builder()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                    .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
                    .propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
                    .build();
        }
    }

    private static class Node {
        final Node parent;
        final ArtifactKey gact;
        final int id;
        boolean present;
        int runtimeCp;
        List<Node> children = new ArrayList<>(0);

        Node(Node parent, ArtifactKey gact, int id) {
            this.parent = parent;
            this.gact = gact;
            this.id = id;
        }

        Node newChild(ArtifactKey gact, int id) {
            final Node child = new Node(this, gact, id);
            children.add(child);
            return child;
        }

        List<ArtifactKey> collectMissingDeploymentDeps(Logger log) {
            final List<ArtifactKey> missing = new ArrayList<>();
            handleChildren(log, 0, missing, (log1, depth, n, collected) -> {
                final StringBuilder buf = new StringBuilder();
                if (n.present) {
                    buf.append('+');
                } else {
                    buf.append('-');
                    collected.add(n.gact);
                }
                buf.append(' ');
                buf.append("    ".repeat(Math.max(0, depth)));
                buf.append(n.gact);
                log1.error(buf.toString());
            });
            return missing;
        }

        List<ArtifactKey> collectDeploymentsOnRtCp(Logger log) {
            final List<ArtifactKey> missing = new ArrayList<>();
            handleChildren(log, 0, missing, (log1, depth, n, collected) -> {
                if (n.runtimeCp == 0) {
                    return;
                }
                final StringBuilder buf = new StringBuilder();
                if (n.runtimeCp == 1) {
                    buf.append(' ');
                } else {
                    buf.append('*');
                    collected.add(n.gact);
                }
                buf.append(' ');
                buf.append("    ".repeat(Math.max(0, depth)));
                buf.append(n.gact);
                log1.error(buf.toString());
            });
            return missing;
        }

        private void handle(Logger log, int depth, List<ArtifactKey> collected, NodeHandler handler) {
            handler.handle(log, depth, this, collected);
            handleChildren(log, depth, collected, handler);
        }

        private void handleChildren(Logger log, int depth, List<ArtifactKey> collected, NodeHandler handler) {
            for (Node child : children) {
                child.handle(log, depth + 1, collected, handler);
            }
        }
    }

    private static class RootNode extends Node {
        final Map<ArtifactKey, Node> expectedDeploymentNodes = new HashMap<>();
        final Set<ArtifactKey> directRuntimeDeps = new HashSet<>();
        final Set<ArtifactKey> allRtDeps = new HashSet<>();
        final Set<ArtifactKey> allDeploymentDeps = new HashSet<>();
        final Map<ArtifactKey, ArtifactKey> unexpectedDeploymentDeps = new HashMap<>(0);

        int deploymentDepsTotal = 1;
        List<ArtifactKey> deploymentsOnRtCp = new ArrayList<>(0);

        RootNode(ArtifactKey gact, int id) {
            super(null, gact, id);
        }

        boolean hasErrors() {
            return deploymentDepsTotal != 0 || runtimeCp != 0 || !unexpectedDeploymentDeps.isEmpty()
                    || !deploymentsOnRtCp.isEmpty();
        }
    }

    private interface NodeHandler {
        void handle(Logger log, int depth, Node n, List<ArtifactKey> collected);
    }
}
