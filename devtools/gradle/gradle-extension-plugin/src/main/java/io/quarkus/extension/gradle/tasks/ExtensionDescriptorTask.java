package io.quarkus.extension.gradle.tasks;

import static io.quarkus.extension.gradle.tasks.Util.artifactType;
import static io.quarkus.extension.gradle.tasks.Util.classifier;
import static io.quarkus.extension.gradle.tasks.Util.isExtension;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.devtools.project.extensions.ScmInfoProvider;
import io.quarkus.extension.gradle.QuarkusExtensionConfiguration;
import io.quarkus.extension.gradle.dsl.Capability;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.platform.tools.ExtensionMetadataValidator;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Task that generates extension descriptor files.
 */
@DisableCachingByDefault(because = "Not cacheable")
public abstract class ExtensionDescriptorTask extends DefaultTask {

    private final FileCollection inputResourcesDirs;

    private static final String GROUP_ID = "group-id";
    private static final String ARTIFACT_ID = "artifact-id";
    private static final String METADATA = "metadata";

    private final Map<String, String> projectInfo;

    @Inject
    public ExtensionDescriptorTask(QuarkusExtensionConfiguration quarkusExtensionConfiguration, SourceSet mainSourceSet,
            Configuration runtimeClasspath) {

        setDescription("Generate extension descriptor file");
        setGroup("quarkus");

        this.inputResourcesDirs = mainSourceSet.getResources().getSourceDirectories();
        getOutputDirectory().convention(
                getProject().getLayout().getBuildDirectory().dir("generated/resources/quarkus-extension/main"));
        getExtensionPropertiesFile().set(
                getOutputDirectory().file(BootstrapConstants.META_INF + "/" + BootstrapConstants.DESCRIPTOR_FILE_NAME));
        getExtensionPropertiesFile().disallowChanges();
        getExtensionDescriptorFile().set(
                getOutputDirectory().file(BootstrapConstants.META_INF + "/" + BootstrapConstants.QUARKUS_EXTENSION_FILE_NAME));
        getExtensionDescriptorFile().disallowChanges();
        getExtensionJsonDescriptorFile().set(
                getOutputDirectory().file(
                        BootstrapConstants.META_INF + "/" + BootstrapConstants.QUARKUS_EXTENSION_JSON_FILE_NAME));
        getExtensionJsonDescriptorFile().disallowChanges();

        var runtimeClasspathArtifacts = runtimeClasspath.getIncoming().getArtifacts();
        getClasspath().from(runtimeClasspathArtifacts.getArtifactFiles());
        getResolvedArtifacts().set(runtimeClasspathArtifacts.getResolvedArtifacts());
        getDeploymentArtifact().set(quarkusExtensionConfiguration.getDeploymentArtifact()
                .orElse(defaultDeploymentArtifactName()));
        getConditionalDependencies().set(quarkusExtensionConfiguration.getConditionalDependencies());
        getConditionalDevDependencies().set(quarkusExtensionConfiguration.getConditionalDevDependencies());
        getDependencyConditions().set(quarkusExtensionConfiguration.getDependencyConditions());
        getParentFirstArtifacts().set(quarkusExtensionConfiguration.getParentFirstArtifacts());
        getRunnerParentFirstArtifacts().set(quarkusExtensionConfiguration.getRunnerParentFirstArtifacts());
        getExcludedArtifacts().set(quarkusExtensionConfiguration.getExcludedArtifacts());
        getLesserPriorityArtifacts().set(quarkusExtensionConfiguration.getLesserPriorityArtifacts());
        getProvidedCapabilities().set(getProject().provider(
                () -> capabilityInputs(quarkusExtensionConfiguration.getProvidedCapabilities())));
        getRequiredCapabilities().set(getProject().provider(
                () -> capabilityInputs(quarkusExtensionConfiguration.getRequiredCapabilities())));
        getRemovedResources().set(getProject().provider(
                () -> removedResourceInputs(quarkusExtensionConfiguration.getRemoveResources())));

        projectInfo = new HashMap<>();
        projectInfo.put("name", getProject().getName());
        if (getProject().getDescription() != null) {
            projectInfo.put("description", getProject().getDescription());
        }
        projectInfo.put("group", getProject().getGroup().toString());
        projectInfo.put("version", getProject().getVersion().toString());
    }

    private String defaultDeploymentArtifactName() {
        String projectName = getProject().getName();
        if (getProject().getParent() != null && projectName.equals("runtime")) {
            projectName = getProject().getParent().getName();
        }
        return String.format("%s:%s-deployment:%s", getProject().getGroup(), projectName, getProject().getVersion());
    }

    @Classpath
    public abstract ConfigurableFileCollection getClasspath();

    @Internal
    public abstract SetProperty<ResolvedArtifactResult> getResolvedArtifacts();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public FileCollection getInputResourcesDirs() {
        return inputResourcesDirs;
    }

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @Internal
    public abstract RegularFileProperty getExtensionPropertiesFile();

    @Internal
    public abstract RegularFileProperty getExtensionDescriptorFile();

    @Internal
    public abstract RegularFileProperty getExtensionJsonDescriptorFile();

    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();

    @Input
    public Map<String, String> getProjectInfo() {
        return projectInfo;
    }

    @Input
    public abstract Property<String> getDeploymentArtifact();

    @Input
    public abstract ListProperty<String> getConditionalDependencies();

    @Input
    public abstract ListProperty<String> getConditionalDevDependencies();

    @Input
    public abstract ListProperty<String> getDependencyConditions();

    @Input
    public abstract ListProperty<String> getParentFirstArtifacts();

    @Input
    public abstract ListProperty<String> getRunnerParentFirstArtifacts();

    @Input
    public abstract ListProperty<String> getExcludedArtifacts();

    @Input
    public abstract ListProperty<String> getLesserPriorityArtifacts();

    @Input
    public abstract ListProperty<String> getProvidedCapabilities();

    @Input
    public abstract ListProperty<String> getRequiredCapabilities();

    @Input
    public abstract ListProperty<String> getRemovedResources();

    @TaskAction
    public void generateExtensionDescriptor() throws IOException {
        getFileSystemOperations().delete(delete -> delete.delete(getOutputDirectory()));
        Path outputMetaInfDir = getExtensionPropertiesFile().get().getAsFile().toPath().getParent();

        generateQuarkusExtensionProperties(outputMetaInfDir);
        generateQuarkusExtensionDescriptor(outputMetaInfDir);
    }

    private void generateQuarkusExtensionProperties(Path metaInfDir) {
        final Properties props = new Properties();
        String deploymentArtifact = getDeploymentArtifact().get();

        props.setProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT, deploymentArtifact);

        setConditionalDepsProperty(BootstrapConstants.CONDITIONAL_DEPENDENCIES,
                getConditionalDependencies().get(), props);
        setConditionalDepsProperty(BootstrapConstants.CONDITIONAL_DEV_DEPENDENCIES,
                getConditionalDevDependencies().get(), props);

        List<String> dependencyConditions = getDependencyConditions().get();
        if (!dependencyConditions.isEmpty()) {
            final StringBuilder buf = new StringBuilder();
            int i = 0;
            buf.append(GACT.fromString(dependencyConditions.get(i++)).toGacString());
            while (i < dependencyConditions.size()) {
                buf.append(' ').append(GACT.fromString(dependencyConditions.get(i++)).toGacString());
            }
            props.setProperty(BootstrapConstants.DEPENDENCY_CONDITION, buf.toString());
        }

        List<String> parentFirstArtifacts = getParentFirstArtifacts().get();
        if (!parentFirstArtifacts.isEmpty()) {
            String val = String.join(",", parentFirstArtifacts);
            props.put(ApplicationModelBuilder.PARENT_FIRST_ARTIFACTS, val);
        }

        List<String> runnerParentFirstArtifacts = getRunnerParentFirstArtifacts().get();
        if (!runnerParentFirstArtifacts.isEmpty()) {
            String val = String.join(",", runnerParentFirstArtifacts);
            props.put(ApplicationModelBuilder.RUNNER_PARENT_FIRST_ARTIFACTS, val);
        }

        List<String> excludedArtifacts = getExcludedArtifacts().get();
        if (!excludedArtifacts.isEmpty()) {
            String val = String.join(",", excludedArtifacts);
            props.put(ApplicationModelBuilder.EXCLUDED_ARTIFACTS, val);
        }

        List<String> lesserPriorityArtifacts = getLesserPriorityArtifacts().get();
        if (!lesserPriorityArtifacts.isEmpty()) {
            String val = String.join(",", lesserPriorityArtifacts);
            props.put(ApplicationModelBuilder.LESSER_PRIORITY_ARTIFACTS, val);
        }

        List<String> providedCapabilities = getProvidedCapabilities().get();
        if (!providedCapabilities.isEmpty()) {
            props.setProperty(BootstrapConstants.PROP_PROVIDES_CAPABILITIES,
                    String.join(",", providedCapabilities));
        }

        List<String> requiredCapabilities = getRequiredCapabilities().get();
        if (!requiredCapabilities.isEmpty()) {
            props.setProperty(BootstrapConstants.PROP_REQUIRES_CAPABILITIES,
                    String.join(",", requiredCapabilities));
        }

        List<String> removedResourcesList = getRemovedResources().get();
        if (!removedResourcesList.isEmpty()) {
            for (String removedResource : removedResourcesList) {
                int equals = removedResource.indexOf('=');
                String artifactName = removedResource.substring(0, equals);
                List<String> removedResources = List.of(removedResource.substring(equals + 1).split(","));
                if (removedResources.isEmpty()) {
                    continue;
                }
                final ArtifactKey key;
                try {
                    key = ArtifactKey.fromString(artifactName);
                } catch (IllegalArgumentException e) {
                    throw new GradleException(
                            "Failed to parse removed resource '" + artifactName, e);
                }
                final String value;
                if (removedResources.size() == 1) {
                    value = removedResources.get(0);
                } else {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(removedResources.get(0));
                    for (int i = 1; i < removedResources.size(); ++i) {
                        final String resource = removedResources.get(i);
                        if (!resource.isBlank()) {
                            sb.append(',').append(resource);
                        }
                    }
                    value = sb.toString();
                }
                props.setProperty(ApplicationModelBuilder.REMOVED_RESOURCES_DOT + key, value);
            }
        }

        try {
            Files.createDirectories(metaInfDir);
            try (BufferedWriter writer = Files
                    .newBufferedWriter(metaInfDir.resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME))) {
                props.store(writer, "Generated by extension-descriptor");
            }
        } catch (IOException e) {
            throw new GradleException(
                    "Failed to persist extension descriptor " + metaInfDir.resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME),
                    e);
        }
    }

    private static void setConditionalDepsProperty(String propName, List<String> conditionalDependencies, Properties props) {
        if (conditionalDependencies != null && !conditionalDependencies.isEmpty()) {
            final StringBuilder buf = new StringBuilder();
            int i = 0;
            buf.append(ArtifactCoords.fromString(conditionalDependencies.get(i++)));
            while (i < conditionalDependencies.size()) {
                buf.append(' ').append(ArtifactCoords.fromString(conditionalDependencies.get(i++)));
            }
            props.setProperty(propName, buf.toString());
        }
    }

    private void generateQuarkusExtensionDescriptor(Path outputMetaInfDirectory)
            throws IOException {
        File extensionFile = getInputExtensionDescriptorFile();

        ObjectMapper mapper = getMapper();
        ObjectNode extObject;
        if (extensionFile != null && extensionFile.exists()) {
            extObject = readExtensionFile(extensionFile.toPath(), mapper);
        } else {
            extObject = mapper.createObjectNode();
        }

        computeArtifactCoords(extObject);
        computeProjectName(extObject);
        computeSourceLocation(extObject);
        computeQuarkusCoreVersion(extObject);
        computeQuarkusExtensions(extObject);

        if (!extObject.has("description") && projectInfo.containsKey("description")) {
            extObject.put("description", projectInfo.get("description"));
        }

        try {
            ExtensionMetadataValidator.validate(extObject);
        } catch (IOException e) {
            throw new GradleException(e.getMessage(), e.getCause());
        }

        final DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

        try (BufferedWriter bw = Files
                .newBufferedWriter(outputMetaInfDirectory.resolve(BootstrapConstants.QUARKUS_EXTENSION_FILE_NAME))) {
            bw.write(getMapper().writer().with(prettyPrinter).writeValueAsString(extObject));
        } catch (IOException e) {
            throw new GradleException(
                    "Failed to persist " + outputMetaInfDirectory.resolve(BootstrapConstants.QUARKUS_EXTENSION_FILE_NAME), e);
        }

        ObjectMapper jsonMapper = getJsonMapper();
        try (BufferedWriter bw = Files
                .newBufferedWriter(outputMetaInfDirectory.resolve(BootstrapConstants.QUARKUS_EXTENSION_JSON_FILE_NAME))) {
            bw.write(jsonMapper.writer().with(prettyPrinter).writeValueAsString(extObject));
        } catch (IOException e) {
            throw new GradleException(
                    "Failed to persist "
                            + outputMetaInfDirectory.resolve(BootstrapConstants.QUARKUS_EXTENSION_JSON_FILE_NAME),
                    e);
        }
    }

    private void computeProjectName(ObjectNode extObject) {
        if (!extObject.has("name")) {
            if (projectInfo.containsKey("name")) {
                extObject.put("name", projectInfo.get("name"));
            } else {
                JsonNode node = extObject.get(ARTIFACT_ID);
                String defaultName = node.asString();
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
                getLogger().warn("Extension name has not been provided for " + extObject.get(GROUP_ID).asString("") + ":"
                        + extObject.get(ARTIFACT_ID).asString("") + "! Using '" + defaultName
                        + "' as the default one.");
                extObject.put("name", defaultName);
            }
        }
    }

    private void computeArtifactCoords(ObjectNode extObject) {
        String groupId = null;
        String artifactId = null;
        String version = null;
        final JsonNode artifactNode = extObject.get("artifact");

        if (artifactNode == null) {
            groupId = extObject.has("groupId") ? extObject.get("groupId").asString() : null;
            artifactId = extObject.has("artifactId") ? extObject.get("artifactId").asString() : null;
            version = extObject.has("version") ? extObject.get("version").asString() : null;
        } else {
            final String[] coordsArr = artifactNode.asString().split(":");
            if (coordsArr.length > 0) {
                groupId = coordsArr[0];
                if (coordsArr.length > 1) {
                    artifactId = coordsArr[1];
                    if (coordsArr.length > 2) {
                        version = coordsArr[2];
                    }
                }
            }
        }
        if (artifactNode == null || groupId == null || artifactId == null || version == null) {
            final ArtifactCoords coords = ArtifactCoords.jar(
                    groupId == null ? projectInfo.get("group") : groupId,
                    artifactId == null ? projectInfo.get("name") : artifactId,
                    version == null ? projectInfo.get("version") : version);
            extObject.put("artifact", coords.toString());
        }
    }

    private void computeSourceLocation(ObjectNode extObject) {
        Map<String, String> repo = new ScmInfoProvider(null).getSourceRepo();
        if (repo != null) {
            ObjectNode metadata = getMetadataNode(extObject);

            for (Map.Entry<String, String> e : repo.entrySet()) {
                metadata.put("scm-" + e.getKey(), e.getValue());

            }
        }
    }

    private void computeQuarkusCoreVersion(ObjectNode extObject) {
        String coreVersion = getQuarkusCoreVersionOrNull();
        if (coreVersion != null) {
            ObjectNode metadata = getMetadataNode(extObject);
            metadata.put("built-with-quarkus-core", coreVersion);
        }
    }

    private static void appendCapability(Capability capability, StringBuilder buf) {
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

    private static List<String> capabilityInputs(List<Capability> capabilities) {
        List<String> inputs = new ArrayList<>(capabilities.size());
        for (Capability capability : capabilities) {
            StringBuilder input = new StringBuilder();
            appendCapability(capability, input);
            inputs.add(input.toString());
        }
        return inputs;
    }

    private static List<String> removedResourceInputs(List<io.quarkus.extension.gradle.dsl.RemovedResource> removedResources) {
        List<String> inputs = new ArrayList<>(removedResources.size());
        for (var removedResource : removedResources) {
            if (removedResource.getRemovedResources().isEmpty()) {
                continue;
            }
            inputs.add(removedResource.getArtifactName() + "="
                    + String.join(",", removedResource.getRemovedResources()));
        }
        return inputs;
    }

    private File getInputExtensionDescriptorFile() {
        for (File inputResourcesDir : getInputResourcesDirs().getFiles()) {
            File extensionDescriptor = inputResourcesDir.toPath()
                    .resolve(BootstrapConstants.META_INF)
                    .resolve(BootstrapConstants.QUARKUS_EXTENSION_FILE_NAME)
                    .toFile();
            if (extensionDescriptor.exists()) {
                return extensionDescriptor;
            }
        }
        return null;
    }

    private void computeQuarkusExtensions(ObjectNode extObject) {
        ObjectNode metadataNode = getMetadataNode(extObject);
        Set<String> extensions = new TreeSet<>();
        for (ResolvedArtifactResult resolvedArtifact : getResolvedArtifacts().get()) {
            if ("jar".equals(artifactType(resolvedArtifact))) {
                Path p = resolvedArtifact.getFile().toPath();
                if (Files.isDirectory(p) && isExtension(p)) {
                    addExtensionCoordinate(extensions, resolvedArtifact);
                } else {
                    try (FileSystem fs = ZipUtils.newFileSystem(p)) {
                        if (isExtension(fs.getPath(""))) {
                            addExtensionCoordinate(extensions, resolvedArtifact);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read " + p, e);
                    }
                }
            }
        }
        ArrayNode extensionArray = metadataNode.putArray("extension-dependencies");
        for (String extension : extensions) {
            extensionArray.add(extension);
        }
    }

    private static void addExtensionCoordinate(Set<String> extensions, ResolvedArtifactResult extension) {
        if (extension.getId().getComponentIdentifier() instanceof ModuleComponentIdentifier id) {
            extensions.add(ArtifactKey.of(id.getGroup(), id.getModule(),
                    classifier(id.getModule(), id.getVersion(), extension.getFile()),
                    artifactType(extension)).toGacString());
        }
    }

    private String getQuarkusCoreVersionOrNull() {
        for (ResolvedArtifactResult resolvedArtifact : getResolvedArtifacts().get()) {
            if (resolvedArtifact.getId().getComponentIdentifier() instanceof ModuleComponentIdentifier artifactId
                    && artifactId.getGroup().equals("io.quarkus") && artifactId.getModule().equals("quarkus-core")) {
                return artifactId.getVersion();
            }
        }
        return null;
    }

    private ObjectMapper getMapper() {
        return YAMLMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
                .build();
    }

    private ObjectMapper getJsonMapper() {
        return JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
                .build();
    }

    private ObjectNode getMetadataNode(ObjectNode extObject) {
        JsonNode mvalue = extObject.get(METADATA);
        if (mvalue != null && mvalue.isObject()) {
            return (ObjectNode) mvalue;
        } else {
            return extObject.putObject(METADATA);
        }
    }

    private ObjectNode readExtensionFile(Path extensionFile, ObjectMapper mapper) throws IOException {
        try (InputStream is = Files.newInputStream(extensionFile)) {
            return mapper.readValue(is, ObjectNode.class);
        } catch (IOException io) {
            throw new IOException("Failed to parse " + extensionFile, io);
        }
    }
}
