package io.quarkus.extension.gradle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.ClasspathNormalizer;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSet;
import org.gradle.language.jvm.tasks.ProcessResources;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.devtools.project.extensions.ScmInfoProvider;
import io.quarkus.extension.gradle.dsl.Capability;
import io.quarkus.extension.gradle.dsl.RemovedResource;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.platform.tools.ExtensionMetadataValidator;

/**
 * Decorating the ProcessResources Task and additionally generate extension descriptor files.
 */
public class ExtensionProcessResourcesTaskDecorator {

    private final Project project;
    private final ProcessResources task;
    private final QuarkusExtensionConfiguration quarkusExtensionConfiguration;
    private final Configuration classpath;
    private final FileCollection inputResourcesDirs;
    private final File outputResourcesDir;

    private static final String GROUP_ID = "group-id";
    private static final String ARTIFACT_ID = "artifact-id";
    private static final String METADATA = "metadata";

    private final Map<String, String> projectInfo;

    public ExtensionProcessResourcesTaskDecorator(
            Project project,
            ProcessResources task,
            QuarkusExtensionConfiguration quarkusExtensionConfiguration,
            SourceSet mainSourceSet,
            Configuration runtimeClasspath) {

        this.project = project;
        this.task = task;
        this.quarkusExtensionConfiguration = quarkusExtensionConfiguration;
        this.outputResourcesDir = mainSourceSet.getOutput().getResourcesDir();
        this.inputResourcesDirs = mainSourceSet.getResources().getSourceDirectories();
        this.classpath = runtimeClasspath;

        projectInfo = new HashMap<>();
        projectInfo.put("name", getProject().getName());
        if (getProject().getDescription() != null) {
            projectInfo.put("description", getProject().getDescription());
        }
        projectInfo.put("group", getProject().getGroup().toString());
        projectInfo.put("version", getProject().getVersion().toString());
    }

    public void decorate() {
        task.notCompatibleWithConfigurationCache("The Quarkus Extension Plugin isn't compatible with the configuration cache");
        registerInputs();
        task.doLast(task -> {
            try {
                generateExtensionDescriptor();
            } catch (IOException e) {
                throw new GradleException("Failed to generate extension descriptor", e);
            }
        });
        task.doFirst(task -> {
            System.out.println("INPUTS : " + task.getInputs());
            System.out.println("OUTPUTS: " + task.getOutputs());
        });
    }

    private void registerInputs() {
        task.from(project.getLayout().getProjectDirectory().file("build.gradle")); // prevent skipping with NO-SOURCE
        task.filesMatching("build.gradle", details -> details.exclude());
        task.getInputs().files(getInputResourcesDirs()).withPropertyName("resourceDirs")
                .withPathSensitivity(PathSensitivity.RELATIVE)
                .skipWhenEmpty();
        task.getInputs().files(getClasspath()).withPropertyName("classpath")
                .withNormalizer(ClasspathNormalizer.class)
                .skipWhenEmpty();
        task.getInputs().property("projectInfo", getProjectInfo());
        task.getInputs().property("deploymentArtifact", getDeploymentArtifact());
        task.getInputs().property("conditionalDependencies", getConditionalDependencies());
        task.getInputs().property("conditionalDevDependencies", getConditionalDevDependencies());
        task.getInputs().property("dependencyConditions", getDependencyConditions());
        task.getInputs().property("parentFirstArtifacts", getParentFirstArtifacts());
        task.getInputs().property("runnerParentFirstArtifacts", getRunnerParentFirstArtifacts());
        task.getInputs().property("excludedArtifacts", getExcludedArtifacts());
        task.getInputs().property("lesserPriorityArtifacts", getLesserPriorityArtifacts());
        task.getInputs().property("providedCapabilities", getProvidedCapabilities());
        task.getInputs().property("requiredCapabilities", getRequiredCapabilities());
        task.getInputs().property("removedResources", getRemovedResources());
        task.getInputs().property("projectName", project.getName());
        if (project.getDescription() != null) {
            task.getInputs().property("projectDescription", project.getDescription());
        }
        task.getInputs().property("projectGroup", project.getGroup().toString());
        task.getInputs().property("projectVersion", project.getVersion().toString());
        task.getOutputs().file(getExtensionPropertiesFile());
        task.getOutputs().file(getExtensionDescriptorFile());
    }

    private Project getProject() {
        return project;
    }

    private Configuration getClasspath() {
        return classpath;
    }

    private FileCollection getInputResourcesDirs() {
        return inputResourcesDirs;
    }

    private File getExtensionPropertiesFile() {
        return outputResourcesDir.toPath()
                .resolve(BootstrapConstants.META_INF)
                .resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME)
                .toFile();
    }

    private File getExtensionDescriptorFile() {
        return outputResourcesDir.toPath()
                .resolve(BootstrapConstants.META_INF)
                .resolve(BootstrapConstants.QUARKUS_EXTENSION_FILE_NAME)
                .toFile();
    }

    private Map<String, String> getProjectInfo() {
        return projectInfo;
    }

    private String getDeploymentArtifact() {
        return quarkusExtensionConfiguration.getDeploymentArtifact()
                .getOrElse(quarkusExtensionConfiguration.getDefaultDeployementArtifactName());
    }

    private List<String> getConditionalDependencies() {
        return quarkusExtensionConfiguration.getConditionalDependencies().get();
    }

    private List<String> getConditionalDevDependencies() {
        return quarkusExtensionConfiguration.getConditionalDevDependencies().get();
    }

    private List<String> getDependencyConditions() {
        return quarkusExtensionConfiguration.getDependencyConditions().get();
    }

    private List<String> getParentFirstArtifacts() {
        return quarkusExtensionConfiguration.getParentFirstArtifacts().get();
    }

    private List<String> getRunnerParentFirstArtifacts() {
        return quarkusExtensionConfiguration.getRunnerParentFirstArtifacts().get();
    }

    private List<String> getExcludedArtifacts() {
        return quarkusExtensionConfiguration.getExcludedArtifacts().get();
    }

    private List<String> getLesserPriorityArtifacts() {
        return quarkusExtensionConfiguration.getLesserPriorityArtifacts().get();
    }

    private List<String> getProvidedCapabilities() {
        return capabilityInputs(quarkusExtensionConfiguration.getProvidedCapabilities());
    }

    private List<String> getRequiredCapabilities() {
        return capabilityInputs(quarkusExtensionConfiguration.getRequiredCapabilities());
    }

    private List<String> getRemovedResources() {
        List<String> removedResources = new ArrayList<>();
        for (RemovedResource removedResource : quarkusExtensionConfiguration.getRemoveResources()) {
            removedResources.add(removedResource.getArtifactName() + "="
                    + String.join(",", removedResource.getRemovedResources()));
        }
        return removedResources;
    }

    private void generateExtensionDescriptor() throws IOException {
        Path outputMetaInfDir = outputResourcesDir.toPath().resolve(BootstrapConstants.META_INF);

        generateQuarkusExtensionProperties(outputMetaInfDir);
        generateQuarkusExtensionDescriptor(outputMetaInfDir);
    }

    private void generateQuarkusExtensionProperties(Path metaInfDir) {
        final Properties props = new Properties();
        String deploymentArtifact = getDeploymentArtifact();

        props.setProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT, deploymentArtifact);

        setConditionalDepsProperty(BootstrapConstants.CONDITIONAL_DEPENDENCIES,
                getConditionalDependencies(), props);
        setConditionalDepsProperty(BootstrapConstants.CONDITIONAL_DEV_DEPENDENCIES,
                getConditionalDevDependencies(), props);

        List<String> dependencyConditions = getDependencyConditions();
        if (dependencyConditions != null && !dependencyConditions.isEmpty()) {
            final StringBuilder buf = new StringBuilder();
            int i = 0;
            buf.append(GACT.fromString(dependencyConditions.get(i++)).toGacString());
            while (i < dependencyConditions.size()) {
                buf.append(' ').append(GACT.fromString(dependencyConditions.get(i++)).toGacString());
            }
            props.setProperty(BootstrapConstants.DEPENDENCY_CONDITION, buf.toString());
        }

        List<String> parentFirstArtifacts = getParentFirstArtifacts();
        if (parentFirstArtifacts != null && !parentFirstArtifacts.isEmpty()) {
            String val = String.join(",", parentFirstArtifacts);
            props.put(ApplicationModelBuilder.PARENT_FIRST_ARTIFACTS, val);
        }

        List<String> runnerParentFirstArtifacts = getRunnerParentFirstArtifacts();
        if (runnerParentFirstArtifacts != null && !runnerParentFirstArtifacts.isEmpty()) {
            String val = String.join(",", runnerParentFirstArtifacts);
            props.put(ApplicationModelBuilder.RUNNER_PARENT_FIRST_ARTIFACTS, val);
        }

        List<String> excludedArtifacts = getExcludedArtifacts();
        if (excludedArtifacts != null && !excludedArtifacts.isEmpty()) {
            String val = String.join(",", excludedArtifacts);
            props.put(ApplicationModelBuilder.EXCLUDED_ARTIFACTS, val);
        }

        List<String> lesserPriorityArtifacts = getLesserPriorityArtifacts();
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
                task.getLogger().warn("Extension name has not been provided for " + extObject.get(GROUP_ID).asText("") + ":"
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
        List<String> extensionList = new ArrayList<>(extensions.size());
        for (ResolvedArtifact extension : extensions) {
            ModuleVersionIdentifier id = extension.getModuleVersion().getId();
            extensionList
                    .add(ArtifactKey.of(id.getGroup(), id.getName(), extension.getClassifier(), extension.getExtension())
                            .toGacString());
        }
        Collections.sort(extensionList); // ensure deterministic order
        for (String extension : extensionList) {
            extensionArray.add(extension);
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
