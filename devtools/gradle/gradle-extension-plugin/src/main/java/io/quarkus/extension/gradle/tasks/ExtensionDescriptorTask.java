package io.quarkus.extension.gradle.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.SourceSet;
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
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.devtools.project.extensions.ScmInfoProvider;
import io.quarkus.extension.gradle.QuarkusExtensionConfiguration;
import io.quarkus.extension.gradle.dsl.Capability;
import io.quarkus.extension.gradle.dsl.RemovedResource;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACT;

/**
 * Task that generates extension descriptor files.
 */
public class ExtensionDescriptorTask extends DefaultTask {

    private final QuarkusExtensionConfiguration quarkusExtensionConfiguration;
    private final Configuration classpath;
    private final File outputResourcesDir;
    private final String inputResourcesDir;

    private static final String GROUP_ID = "group-id";
    private static final String ARTIFACT_ID = "artifact-id";
    private static final String METADATA = "metadata";

    private final Map<String, String> projectInfo;

    @Inject
    public ExtensionDescriptorTask(QuarkusExtensionConfiguration quarkusExtensionConfiguration, SourceSet mainSourceSet,
            Configuration runtimeClasspath) {

        setDescription("Generate extension descriptor file");
        setGroup("quarkus");

        this.quarkusExtensionConfiguration = quarkusExtensionConfiguration;
        this.outputResourcesDir = mainSourceSet.getOutput().getResourcesDir();
        this.inputResourcesDir = mainSourceSet.getResources().getSourceDirectories().getAsPath();
        this.classpath = runtimeClasspath;

        // Calling this method tells Gradle that it should not fail the build. Side effect is that the configuration
        // cache will be at least degraded, but the build will not fail.
        notCompatibleWithConfigurationCache("The Quarkus Extension Plugin isn't compatible with the configuration cache");

        projectInfo = new HashMap<>();
        projectInfo.put("name", getProject().getName());
        if (getProject().getDescription() != null) {
            projectInfo.put("description", getProject().getDescription());
        }
        projectInfo.put("group", getProject().getGroup().toString());
        projectInfo.put("version", getProject().getVersion().toString());
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
        String deploymentArtifact = quarkusExtensionConfiguration.getDeploymentArtifact()
                .getOrElse(quarkusExtensionConfiguration.getDefaultDeployementArtifactName());

        props.setProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT, deploymentArtifact);

        setConditionalDepsProperty(BootstrapConstants.CONDITIONAL_DEPENDENCIES,
                quarkusExtensionConfiguration.getConditionalDependencies().get(), props);
        setConditionalDepsProperty(BootstrapConstants.CONDITIONAL_DEV_DEPENDENCIES,
                quarkusExtensionConfiguration.getConditionalDevDependencies().get(), props);

        List<String> dependencyConditions = quarkusExtensionConfiguration.getDependencyConditions().get();
        if (dependencyConditions != null && !dependencyConditions.isEmpty()) {
            final StringBuilder buf = new StringBuilder();
            int i = 0;
            buf.append(GACT.fromString(dependencyConditions.get(i++)).toGacString());
            while (i < dependencyConditions.size()) {
                buf.append(' ').append(GACT.fromString(dependencyConditions.get(i++)).toGacString());
            }
            props.setProperty(BootstrapConstants.DEPENDENCY_CONDITION, buf.toString());
        }

        List<String> parentFirstArtifacts = quarkusExtensionConfiguration.getParentFirstArtifacts().get();
        if (parentFirstArtifacts != null && !parentFirstArtifacts.isEmpty()) {
            String val = String.join(",", parentFirstArtifacts);
            props.put(ApplicationModelBuilder.PARENT_FIRST_ARTIFACTS, val);
        }

        List<String> runnerParentFirstArtifacts = quarkusExtensionConfiguration.getRunnerParentFirstArtifacts().get();
        if (runnerParentFirstArtifacts != null && !runnerParentFirstArtifacts.isEmpty()) {
            String val = String.join(",", runnerParentFirstArtifacts);
            props.put(ApplicationModelBuilder.RUNNER_PARENT_FIRST_ARTIFACTS, val);
        }

        List<String> excludedArtifacts = quarkusExtensionConfiguration.getExcludedArtifacts().get();
        if (excludedArtifacts != null && !excludedArtifacts.isEmpty()) {
            String val = String.join(",", excludedArtifacts);
            props.put(ApplicationModelBuilder.EXCLUDED_ARTIFACTS, val);
        }

        List<String> lesserPriorityArtifacts = quarkusExtensionConfiguration.getLesserPriorityArtifacts().get();
        if (lesserPriorityArtifacts != null && !lesserPriorityArtifacts.isEmpty()) {
            String val = String.join(",", lesserPriorityArtifacts);
            props.put(ApplicationModelBuilder.LESSER_PRIORITY_ARTIFACTS, val);
        }

        if (!quarkusExtensionConfiguration.getProvidedCapabilities().isEmpty()) {
            final StringBuilder buf = new StringBuilder();
            final Iterator<Capability> i = quarkusExtensionConfiguration.getProvidedCapabilities().iterator();
            appendCapability(i.next(), buf);
            while (i.hasNext()) {
                appendCapability(i.next(), buf.append(','));
            }
            props.setProperty(BootstrapConstants.PROP_PROVIDES_CAPABILITIES, buf.toString());
        }

        if (!quarkusExtensionConfiguration.getRequiredCapabilities().isEmpty()) {
            final StringBuilder buf = new StringBuilder();
            final Iterator<Capability> i = quarkusExtensionConfiguration.getRequiredCapabilities().iterator();
            appendCapability(i.next(), buf);
            while (i.hasNext()) {
                appendCapability(i.next(), buf.append(','));
            }
            props.setProperty(BootstrapConstants.PROP_REQUIRES_CAPABILITIES, buf.toString());
        }

        if (!quarkusExtensionConfiguration.getRemoveResources().isEmpty()) {
            for (RemovedResource removedResource : quarkusExtensionConfiguration.getRemoveResources()) {
                final ArtifactKey key;
                try {
                    key = ArtifactKey.fromString(removedResource.getArtifactName());
                } catch (IllegalArgumentException e) {
                    throw new GradleException(
                            "Failed to parse removed resource '" + removedResource.getArtifactName(), e);
                }
                if (removedResource.getRemovedResources().isEmpty()) {
                    continue;
                }
                final List<String> resources = removedResource.getRemovedResources();
                if (resources.size() == 0) {
                    continue;
                }
                final String value;
                if (resources.size() == 1) {
                    value = resources.get(0);
                } else {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(resources.get(0));
                    for (int i = 1; i < resources.size(); ++i) {
                        final String resource = resources.get(i);
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
        computeSourceLocation(extObject);
        computeQuarkusCoreVersion(extObject);
        computeQuarkusExtensions(extObject);

        if (!extObject.has("description") && projectInfo.containsKey("description")) {
            extObject.put("description", projectInfo.get("description"));
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
            if (projectInfo.containsKey("name")) {
                extObject.put("name", projectInfo.get("name"));
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
                    groupId == null ? projectInfo.get("group") : groupId,
                    artifactId == null ? projectInfo.get("name") : artifactId,
                    null,
                    "jar",
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

    private void computeQuarkusExtensions(ObjectNode extObject) {
        ObjectNode metadataNode = getMetadataNode(extObject);
        Set<ResolvedArtifact> extensions = new HashSet<>();
        for (ResolvedArtifact resolvedArtifact : getClasspath().getResolvedConfiguration().getResolvedArtifacts()) {
            if (resolvedArtifact.getExtension().equals("jar")) {
                Path p = resolvedArtifact.getFile().toPath();
                if (Files.isDirectory(p) && isExtension(p)) {
                    extensions.add(resolvedArtifact);
                } else {
                    try (FileSystem fs = ZipUtils.newFileSystem(p)) {
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

    private static boolean isExtension(Path extensionFile) {
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
