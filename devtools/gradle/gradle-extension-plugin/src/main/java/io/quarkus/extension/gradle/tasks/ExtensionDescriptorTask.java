package io.quarkus.extension.gradle.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.TaskAction;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.extension.gradle.QuarkusExtensionConfiguration;
import io.quarkus.maven.dependency.GACT;

/**
 * Task that generates extension descriptor files.
 */
public class ExtensionDescriptorTask extends DefaultTask {

    private QuarkusExtensionConfiguration quarkusExtensionConfiguration;
    private Configuration classpath;
    private File outputResourcesDir;
    private String inputResourcesDir;

    private static final String GROUP_ID = "group-id";
    private static final String ARTIFACT_ID = "artifact-id";
    private static final String METADATA = "metadata";

    public ExtensionDescriptorTask() {
        setDescription("Generate extension descriptor file");
        setGroup("quarkus");
    }

    public void setOutputResourcesDir(File outputResourcesDir) {
        this.outputResourcesDir = outputResourcesDir;
    }

    public void setInputResourcesDir(String inputResourcesDir) {
        this.inputResourcesDir = inputResourcesDir;
    }

    public void setQuarkusExtensionConfiguration(QuarkusExtensionConfiguration quarkusExtensionConfiguration) {
        this.quarkusExtensionConfiguration = quarkusExtensionConfiguration;
    }

    public void setClasspath(Configuration classpath) {
        this.classpath = classpath;
    }

    @Classpath
    public Configuration getClasspath() {
        return classpath;
    }

    @TaskAction
    public void generateExtensionDescriptor() throws IOException {
        Path outputMetaInfDir = outputResourcesDir.toPath().resolve(BootstrapConstants.META_INF);
        Path inputMetaInfDif = Path.of(inputResourcesDir).resolve(BootstrapConstants.META_INF);

        generateQuarkusExtensionProperties(outputMetaInfDir);
        generateQuarkusExtensionDescriptor(inputMetaInfDif, outputMetaInfDir);
    }

    private void generateQuarkusExtensionProperties(Path metaInfDir) {
        final Properties props = new Properties();
        String deploymentArtifact = quarkusExtensionConfiguration.getDeploymentArtifact();
        if (quarkusExtensionConfiguration.getDeploymentArtifact() == null) {
            deploymentArtifact = quarkusExtensionConfiguration.getDefaultDeployementArtifactName();
        }
        props.setProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT, deploymentArtifact);

        List<String> conditionalDependencies = quarkusExtensionConfiguration.getConditionalDependencies();
        if (conditionalDependencies != null && !conditionalDependencies.isEmpty()) {
            final StringBuilder buf = new StringBuilder();
            int i = 0;
            buf.append(AppArtifactCoords.fromString(conditionalDependencies.get(i++)).toString());
            while (i < conditionalDependencies.size()) {
                buf.append(' ').append(AppArtifactCoords.fromString(conditionalDependencies.get(i++)).toString());
            }
            props.setProperty(BootstrapConstants.CONDITIONAL_DEPENDENCIES, buf.toString());
        }

        List<String> dependencyConditions = quarkusExtensionConfiguration.getDependencyConditions();
        if (dependencyConditions != null && !dependencyConditions.isEmpty()) {
            final StringBuilder buf = new StringBuilder();
            int i = 0;
            buf.append(GACT.fromString(dependencyConditions.get(i++)).toGacString());
            while (i < dependencyConditions.size()) {
                buf.append(' ').append(GACT.fromString(dependencyConditions.get(i++)).toGacString());
            }
            props.setProperty(BootstrapConstants.DEPENDENCY_CONDITION, buf.toString());
        }

        List<String> parentFirstArtifacts = quarkusExtensionConfiguration.getParentFirstArtifacts();
        if (parentFirstArtifacts != null && !parentFirstArtifacts.isEmpty()) {
            String val = String.join(",", parentFirstArtifacts);
            props.put(AppModel.PARENT_FIRST_ARTIFACTS, val);
        }

        List<String> runnerParentFirstArtifacts = quarkusExtensionConfiguration.getRunnerParentFirstArtifacts();
        if (runnerParentFirstArtifacts != null && !runnerParentFirstArtifacts.isEmpty()) {
            String val = String.join(",", runnerParentFirstArtifacts);
            props.put(AppModel.RUNNER_PARENT_FIRST_ARTIFACTS, val);
        }

        List<String> excludedArtifacts = quarkusExtensionConfiguration.getExcludedArtifacts();
        if (excludedArtifacts != null && !excludedArtifacts.isEmpty()) {
            String val = String.join(",", excludedArtifacts);
            props.put(AppModel.EXCLUDED_ARTIFACTS, val);
        }

        List<String> lesserPriorityArtifacts = quarkusExtensionConfiguration.getLesserPriorityArtifacts();
        if (lesserPriorityArtifacts != null && !lesserPriorityArtifacts.isEmpty()) {
            String val = String.join(",", lesserPriorityArtifacts);
            props.put(AppModel.LESSER_PRIORITY_ARTIFACTS, val);
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

    private void generateQuarkusExtensionDescriptor(Path inputMetaInfDirectory, Path outputMetaInfDirectory)
            throws IOException {
        File extensionFile = new File(inputMetaInfDirectory.toFile(), BootstrapConstants.QUARKUS_EXTENSION_FILE_NAME);

        ObjectMapper mapper = getMapper();
        ObjectNode extObject;
        if (extensionFile.exists()) {
            extObject = readExtensionFile(extensionFile.toPath(), mapper);
        } else {
            extObject = mapper.createObjectNode();
        }

        computeArtifactCoords(extObject);
        computeProjectName(extObject);
        computeQuarkusCoreVersion(extObject);
        computeQuarkusExtensions(extObject);

        if (!extObject.has("description") && getProject().getDescription() != null) {
            extObject.put("description", getProject().getDescription());
        }

        final DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

        try (BufferedWriter bw = Files
                .newBufferedWriter(outputMetaInfDirectory.resolve(BootstrapConstants.QUARKUS_EXTENSION_FILE_NAME))) {
            bw.write(getMapper().writer(prettyPrinter).writeValueAsString(extObject));
        } catch (IOException e) {
            throw new GradleException(
                    "Failed to persist " + outputMetaInfDirectory.resolve(BootstrapConstants.QUARKUS_EXTENSION_FILE_NAME), e);
        }
    }

    private void computeProjectName(ObjectNode extObject) {
        if (!extObject.has("name")) {
            if (getProject().getName() != null) {
                extObject.put("name", getProject().getName());
            } else {
                JsonNode node = extObject.get(ARTIFACT_ID);
                String defaultName = node.asText();
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
                getLogger().warn("Extension name has not been provided for " + extObject.get(GROUP_ID).asText("") + ":"
                        + extObject.get(ARTIFACT_ID).asText("") + "! Using '" + defaultName
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
            groupId = extObject.has("groupId") ? extObject.get("groupId").asText() : null;
            artifactId = extObject.has("artifactId") ? extObject.get("artifactId").asText() : null;
            version = extObject.has("version") ? extObject.get("version").asText() : null;
        } else {
            final String[] coordsArr = artifactNode.asText().split(":");
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
            final AppArtifactCoords coords = new AppArtifactCoords(
                    groupId == null ? getProject().getGroup().toString() : groupId,
                    artifactId == null ? getProject().getName() : artifactId,
                    null,
                    "jar",
                    version == null ? getProject().getVersion().toString() : version);
            extObject.put("artifact", coords.toString());
        }
    }

    private void computeQuarkusCoreVersion(ObjectNode extObject) {
        String coreVersion = getQuarkusCoreVersionOrNull();
        if (coreVersion != null) {
            ObjectNode metadata = getMetadataNode(extObject);
            metadata.put("built-with-quarkus-core", coreVersion);
        }
    }

    private void computeQuarkusExtensions(ObjectNode extObject) {
        ObjectNode metadataNode = getMetadataNode(extObject);
        Set<ResolvedArtifact> extensions = new HashSet<>();
        for (ResolvedArtifact resolvedArtifact : getClasspath().getResolvedConfiguration().getResolvedArtifacts()) {
            if (resolvedArtifact.getExtension().equals("jar")) {
                Path p = resolvedArtifact.getFile().toPath();
                if (Files.isDirectory(p) && isExtension(p)) {
                    extensions.add(resolvedArtifact);
                } else {
                    try (FileSystem fs = FileSystems.newFileSystem(p, null)) {
                        if (isExtension(fs.getPath(""))) {
                            extensions.add(resolvedArtifact);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read " + p, e);
                    }
                }
            }
        }
        ArrayNode extensionArray = metadataNode.putArray("extension-dependencies");
        for (ResolvedArtifact extension : extensions) {
            ModuleVersionIdentifier id = extension.getModuleVersion().getId();
            extensionArray
                    .add(new AppArtifactKey(id.getGroup(), id.getName(), extension.getClassifier(), extension.getExtension())
                            .toGacString());
        }
    }

    private String getQuarkusCoreVersionOrNull() {
        for (ResolvedArtifact resolvedArtifact : getClasspath().getResolvedConfiguration().getResolvedArtifacts()) {
            ModuleVersionIdentifier artifactId = resolvedArtifact.getModuleVersion().getId();
            if (artifactId.getGroup().equals("io.quarkus") && artifactId.getName().equals("quarkus-core")) {
                return artifactId.getVersion();
            }
        }
        return null;
    }

    private boolean isExtension(Path extensionFile) {
        final Path p = extensionFile.resolve(BootstrapConstants.DESCRIPTOR_PATH);
        return Files.exists(p);
    }

    private ObjectMapper getMapper() {
        YAMLFactory yf = new YAMLFactory();
        return new ObjectMapper(yf)
                .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
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
