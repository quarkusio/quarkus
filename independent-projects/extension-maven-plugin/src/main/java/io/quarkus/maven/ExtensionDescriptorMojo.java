package io.quarkus.maven;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;
import io.quarkus.bootstrap.util.DependencyUtils;
import io.quarkus.bootstrap.util.PropertyUtils;
import io.quarkus.devtools.project.extensions.ScmInfoProvider;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.maven.capabilities.CapabilitiesConfig;
import io.quarkus.maven.capabilities.CapabilityConfig;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACTV;

/**
 * Generates Quarkus extension descriptor for the runtime artifact.
 * <p>
 * <p/>
 * Also generates META-INF/quarkus-extension.json which includes properties of
 * the extension such as name, labels, maven coordinates, etc that are used by
 * the tools.
 *
 * @author Alexey Loubyansky
 */
@Mojo(name = "extension-descriptor", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class ExtensionDescriptorMojo extends AbstractMojo {

    public static class RemovedResources {
        String key;
        String resources;
    }

    private static final String GROUP_ID = "group-id";
    private static final String ARTIFACT_ID = "artifact-id";
    private static final String METADATA = "metadata";

    /**
     * The entry point to Aether, i.e. the component doing all the work.
     *
     * @component
     */
    @Component
    RepositorySystem repoSystem;

    @Component
    RemoteRepositoryManager remoteRepoManager;

    @Component
    BootstrapWorkspaceProvider workspaceProvider;

    @Parameter(defaultValue = "${session}", readonly = true)
    MavenSession session;

    /**
     * The current repository/network configuration of Maven.
     *
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution of artifacts and
     * their dependencies.
     *
     * @parameter default-value="${project.remoteProjectRepositories}"
     * @readonly
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    /**
     * The directory for compiled classes.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;

    /**
     * Coordinates of the corresponding deployment artifact.
     */
    @Parameter(required = true, defaultValue = "${project.groupId}:${project.artifactId}-deployment:${project.version}")
    private String deployment;

    /**
     * Provided and required <a href="https://quarkus.io/guides/capabilities">extension capabilities</a>.
     */
    @Parameter(required = false)
    CapabilitiesConfig capabilities = new CapabilitiesConfig();

    /**
     * Extension metadata template file
     */
    @Parameter(required = true, defaultValue = "${project.build.outputDirectory}/META-INF/quarkus-extension.yaml")
    private File extensionFile;

    @Parameter(defaultValue = "${project}")
    protected MavenProject project;

    /**
     * Artifacts that should never end up in the final build. Usually this should only be set if we know
     * this extension provides a newer version of a given artifact that is under a different GAV. E.g. this
     * can be used to make sure that the legacy javax API's are not included if an extension is using the new
     * Jakarta version.
     */
    @Parameter
    List<String> excludedArtifacts;

    /**
     * Resources that should excluded from the classloader and the packaged application.
     * It is an equivalent of {@code quarkus.class-loading.removed-resources} from {@code application.properties}
     * but in the `META-INF/quarkus-extension.properties`.
     */
    @Parameter
    List<RemovedResources> removedResources = List.of();

    /**
     * Artifacts that are always loaded parent first when running in dev or test mode. This is an advanced option
     * and should only be used if you are sure that this is the correct solution for the use case.
     * <p>
     * A possible example of this would be logging libraries, as these need to be loaded by the system class loader.
     */
    @Parameter
    List<String> parentFirstArtifacts;

    /**
     * Artifacts that are always loaded parent when the fast-jar is used. This is an advanced option
     * and should only be used if you are sure that this is the correct solution for the use case.
     * <p>
     * A possible example of this would be logging libraries, as these need to be loaded by the system class loader.
     */
    @Parameter
    List<String> runnerParentFirstArtifacts;

    /**
     * Artifacts that will only be used to load a class or resource if no other normal element exists.
     * This is an advanced option that should only be used when there is a case of multiple jars
     * containing the same classes and we need to control which jars is actually used to load the classes.
     */
    @Parameter
    List<String> lesserPriorityArtifacts;

    /**
     * Whether to skip validation of extension's runtime and deployment dependencies.
     */
    @Parameter(required = false, defaultValue = "${skipExtensionValidation}")
    private boolean skipExtensionValidation;

    /**
     * Whether to ignore failure detecting the Quarkus core version used to build the extension,
     * which would be recorded in the extension's metadata.
     */
    @Parameter(required = false, defaultValue = "${ignoreNotDetectedQuarkusCoreVersion}")
    boolean ignoreNotDetectedQuarkusCoreVersion;

    /**
     * <a href="https://quarkus.io/guides/conditional-extension-dependencies">Conditional dependencies</a> that should be
     * enabled in case certain classpath conditions have been satisfied.
     */
    @Parameter
    private List<String> conditionalDependencies = new ArrayList<>(0);

    /**
     * <a href="https://quarkus.io/guides/conditional-extension-dependencies">Extension dependency condition</a> that should be
     * satisfied for this extension to be enabled
     * in case it is added as a conditional dependency of another extension.
     */
    @Parameter
    private List<String> dependencyCondition = new ArrayList<>(0);

    /**
     * Whether to skip validation of the codestart artifact, in case its configured
     */
    @Parameter(property = "skipCodestartValidation")
    boolean skipCodestartValidation;

    @Parameter(defaultValue = "${maven.compiler.release}", readonly = true)
    String minimumJavaVersion;

    ArtifactCoords deploymentCoords;
    CollectResult collectedDeploymentDeps;
    DependencyResult runtimeDeps;

    MavenArtifactResolver resolver;

    @Override
    public void execute() throws MojoExecutionException {

        if (!skipExtensionValidation) {
            validateExtensionDeps();
        }

        if (conditionalDependencies.isEmpty()) {
            // if conditional dependencies haven't been configured
            // we check whether there are direct optional dependencies on extensions
            // that are configured with a dependency condition
            // such dependencies will be registered as conditional
            StringBuilder buf = null;
            for (org.apache.maven.model.Dependency d : project.getDependencies()) {
                if (!d.isOptional()) {
                    continue;
                }
                if (!d.getScope().isEmpty()
                        && !(d.getScope().equals(JavaScopes.COMPILE) || d.getScope().equals(JavaScopes.RUNTIME))) {
                    continue;
                }
                final Properties props = getExtensionDescriptor(
                        new DefaultArtifact(d.getGroupId(), d.getArtifactId(), d.getClassifier(), d.getType(), d.getVersion()),
                        false);
                if (props == null || !props.containsKey(BootstrapConstants.DEPENDENCY_CONDITION)) {
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

        final Properties props = new Properties();
        props.setProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT, deployment);

        if (!conditionalDependencies.isEmpty()) {
            final StringBuilder buf = new StringBuilder();
            int i = 0;
            buf.append(ArtifactCoords.fromString(conditionalDependencies.get(i++)).toString());
            while (i < conditionalDependencies.size()) {
                buf.append(' ').append(ArtifactCoords.fromString(conditionalDependencies.get(i++)).toString());
            }
            props.setProperty(BootstrapConstants.CONDITIONAL_DEPENDENCIES, buf.toString());
        }
        if (!dependencyCondition.isEmpty()) {
            final StringBuilder buf = new StringBuilder();
            int i = 0;
            buf.append(ArtifactKey.fromString(dependencyCondition.get(i++)).toGacString());
            while (i < dependencyCondition.size()) {
                buf.append(' ').append(ArtifactKey.fromString(dependencyCondition.get(i++)).toGacString());
            }
            props.setProperty(BootstrapConstants.DEPENDENCY_CONDITION, buf.toString());

        }

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

        if (parentFirstArtifacts != null && !parentFirstArtifacts.isEmpty()) {
            String val = String.join(",", parentFirstArtifacts);
            props.put(ApplicationModelBuilder.PARENT_FIRST_ARTIFACTS, val);
        }

        if (runnerParentFirstArtifacts != null && !runnerParentFirstArtifacts.isEmpty()) {
            String val = String.join(",", runnerParentFirstArtifacts);
            props.put(ApplicationModelBuilder.RUNNER_PARENT_FIRST_ARTIFACTS, val);
        }

        if (excludedArtifacts != null && !excludedArtifacts.isEmpty()) {
            String val = String.join(",", excludedArtifacts);
            props.put(ApplicationModelBuilder.EXCLUDED_ARTIFACTS, val);
        }

        if (!removedResources.isEmpty()) {
            for (RemovedResources entry : removedResources) {
                final ArtifactKey key;
                try {
                    key = ArtifactKey.fromString(entry.key);
                } catch (IllegalArgumentException e) {
                    throw new MojoExecutionException(
                            "Failed to parse removed resource '" + entry.key + '=' + entry.resources + "'", e);
                }
                if (entry.resources == null || entry.resources.isBlank()) {
                    continue;
                }
                final String[] resources = entry.resources.split(",");
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
                props.setProperty(ApplicationModelBuilder.REMOVED_RESOURCES_DOT + key.toString(), value);
            }
        }

        if (lesserPriorityArtifacts != null && !lesserPriorityArtifacts.isEmpty()) {
            String val = String.join(",", lesserPriorityArtifacts);
            props.put(ApplicationModelBuilder.LESSER_PRIORITY_ARTIFACTS, val);
        }

        final Path output = outputDirectory.toPath().resolve(BootstrapConstants.META_INF);
        try {
            Files.createDirectories(output);
            PropertyUtils.store(props, output.resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME));
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to persist extension descriptor " + output.resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME),
                    e);
        }

        // extension.json
        if (extensionFile == null) {
            extensionFile = output.resolve(BootstrapConstants.QUARKUS_EXTENSION_FILE_NAME).toFile();
        }

        ObjectNode extObject;
        if (!extensionFile.exists()) {
            // if does not exist look for fallback .json
            extensionFile = new File(extensionFile.getParent(), "quarkus-extension.json");
        }

        ObjectMapper mapper = null;
        if (extensionFile.exists()) {
            mapper = getMapper(extensionFile.toString().endsWith(".yaml"));
            extObject = readExtensionDescriptorFile(extensionFile.toPath(), mapper);
        } else {
            mapper = getMapper(true);
            extObject = getMapper(true).createObjectNode();
        }

        transformLegacyToNew(extObject, mapper);

        ensureArtifactCoords(extObject);

        if (extObject.get("name") == null) {
            if (project.getName() != null) {
                extObject.put("name", project.getName());
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
                getLog().warn("Extension name has not been provided for " + extObject.get(GROUP_ID).asText("") + ":"
                        + extObject.get(ARTIFACT_ID).asText("") + "! Using '" + defaultName
                        + "' as the default one.");
                extObject.put("name", defaultName);
            }
        }
        if (!extObject.has("description") && project.getDescription() != null) {
            extObject.put("description", project.getDescription());
        }

        setBuiltWithQuarkusCoreVersion(extObject);
        addJavaVersion(extObject);
        addCapabilities(extObject);
        addSource(extObject);
        addExtensionDependencies(extObject);

        completeCodestartArtifact(mapper, extObject);

        final DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

        try (BufferedWriter bw = Files
                .newBufferedWriter(output.resolve(BootstrapConstants.QUARKUS_EXTENSION_FILE_NAME))) {
            bw.write(getMapper(true).writer(prettyPrinter).writeValueAsString(extObject));
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to persist " + output.resolve(BootstrapConstants.QUARKUS_EXTENSION_FILE_NAME), e);
        }
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
                    groupId == null ? project.getGroupId() : groupId,
                    artifactId == null ? project.getArtifactId() : artifactId,
                    version == null ? project.getVersion() : version);
            extObject.put("artifact", coords.toString());
        }
    }

    private static String getRealValueOrNull(String s, String propertyExpr) {
        return s != null && !s.isBlank() && !s.equals(propertyExpr) ? s : null;
    }

    private ObjectNode readExtensionDescriptorFile(Path extensionFile, ObjectMapper mapper) throws MojoExecutionException {
        try (InputStream is = Files.newInputStream(extensionFile)) {
            return mapper.readValue(is, ObjectNode.class);
        } catch (IOException io) {
            throw new MojoExecutionException("Failed to parse " + extensionFile, io);
        }
    }

    private void completeCodestartArtifact(ObjectMapper mapper, ObjectNode extObject) throws MojoExecutionException {
        JsonNode mvalue = getJsonElement(extObject, METADATA, "codestart");
        if (mvalue == null || !mvalue.isObject()) {
            return;
        }
        final ObjectNode codestartObject = (ObjectNode) mvalue;
        mvalue = mvalue.get("artifact");
        if (mvalue == null) {
            if (!skipCodestartValidation) {
                throw new MojoExecutionException("Codestart artifact is missing from the " + extensionFile);
            }
            return;
        }

        String codestartArtifact = getCodestartArtifact(mvalue.asText(), project.getVersion());
        final ArtifactCoords codestartArtifactCoords = GACTV.fromString(codestartArtifact);
        codestartObject.put("artifact", codestartArtifactCoords.toString());
        if (!skipCodestartValidation) {
            // first we look for it in the workspace, if it's in there we don't need to actually resolve the artifact, because it might not have been built yet
            if (workspaceProvider.getProject(codestartArtifactCoords.getGroupId(),
                    codestartArtifactCoords.getArtifactId()) != null) {
                return;
            }
            for (Artifact attached : project.getAttachedArtifacts()) {
                if (codestartArtifactCoords.getArtifactId().equals(attached.getArtifactId()) &&
                        codestartArtifactCoords.getClassifier().equals(attached.getClassifier()) &&
                        codestartArtifactCoords.getType().equals(attached.getType()) &&
                        codestartArtifactCoords.getVersion().equals(attached.getVersion()) &&
                        codestartArtifactCoords.getGroupId().equals(attached.getGroupId())) {
                    return;
                }
            }
            try {
                resolve(new DefaultArtifact(codestartArtifact));
            } catch (MojoExecutionException e) {
                throw new MojoExecutionException("Failed to resolve codestart artifact " + codestartArtifactCoords, e);
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

    private void setBuiltWithQuarkusCoreVersion(ObjectNode extObject) throws MojoExecutionException {
        final QuarkusCoreDeploymentVersionLocator coreVersionLocator = new QuarkusCoreDeploymentVersionLocator();
        final DependencyNode root;
        try {
            root = repoSystem.collectDependencies(repoSession, newCollectRuntimeDepsRequest()).getRoot();
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to collect runtime dependencies of " + project.getArtifact(), e);
        }
        root.accept(coreVersionLocator);
        if (coreVersionLocator.coreVersion != null) {
            ObjectNode metadata = getMetadataNode(extObject);
            metadata.put("built-with-quarkus-core", coreVersionLocator.coreVersion);
        } else if (!ignoreNotDetectedQuarkusCoreVersion) {
            throw new MojoExecutionException("Failed to determine the Quarkus core version used to build the extension");
        }

    }

    private void addExtensionDependencies(ObjectNode extObject) throws MojoExecutionException {
        final AtomicReference<ArrayNode> extensionDeps = new AtomicReference<>();
        final DependencyVisitor capabilityCollector = new DependencyVisitor() {
            @Override
            public boolean visitEnter(DependencyNode node) {
                final org.eclipse.aether.artifact.Artifact a = node.getArtifact();
                if (a != null && a.getFile() != null && a.getExtension().equals("jar")) {
                    Path p = a.getFile().toPath();
                    boolean isExtension = false;
                    if (Files.isDirectory(p)) {
                        isExtension = getExtensionDescriptorOrNull(p) != null;
                    } else {
                        // in some cases a local dependency might not producing the classes directory
                        // but assembling the JAR directly using maven plugins
                        if (!Files.exists(p)) {
                            final Path workspaceJar = p.getParent().resolve(LocalWorkspace.getFileName(a));
                            if (!Files.exists(workspaceJar)) {
                                getLog().warn("Failed to resolve " + a + ", " + p + " does not exist");
                                return true;
                            }
                            p = workspaceJar;
                        }
                        try (FileSystem fs = ZipUtils.newFileSystem(p)) {
                            isExtension = getExtensionDescriptorOrNull(fs.getPath("")) != null;
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to read " + p, e);
                        }
                    }
                    if (isExtension) {
                        ArrayNode deps = extensionDeps.get();
                        if (deps == null) {
                            deps = getMetadataNode(extObject).putArray("extension-dependencies");
                            extensionDeps.set(deps);
                        }
                        deps.add(ArtifactKey.of(a.getGroupId(), a.getArtifactId(), a.getClassifier(), a.getExtension())
                                .toGacString());
                    }
                }
                return true;
            }

            @Override
            public boolean visitLeave(DependencyNode node) {
                return true;
            }
        };
        final DependencyNode rootNode = resolveRuntimeDeps().getRoot();
        rootNode.accept(capabilityCollector);
    }

    private void addSource(ObjectNode extObject) throws MojoExecutionException {
        Scm scm = getScm();
        String scmUrl = scm != null ? scm.getUrl() : null;

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
                getLog().warn(warning);
            }
        }
        // We had been generic, but go a bit more specific so we can give a sensible message
        else if (!metadata.has("scm-url")) {
            getLog().debug(
                    "Could not work out a source control repository from the build environment or build file. Consider adding an scm-url entry in quarkus-extension.yaml");
        }
    }

    private Scm getScm() {
        // We have three ways to do this; project.getScm() will query the derived model. Sadly, inherited <scm> entries are usually wrong, unless the parent is in the same project
        // We can use getOriginalModel and getParent to walk the tree, but this will miss parents in poms outside the current execution, which might include a local reactor that we'd actually want to query
        // Or we can use the bootstrap provider
        Scm scm = null;
        final Artifact artifact = project.getArtifact();
        LocalProject localProject = workspaceProvider.getProject(artifact.getGroupId(), artifact.getArtifactId());

        if (localProject == null) {
            final Log log = getLog();
            log.debug("Workspace provider could not resolve local project for " + artifact.getGroupId() + ":"
                    + artifact.getArtifactId());
        }

        while (scm == null && localProject != null) {
            scm = localProject.getRawModel().getScm();
            localProject = localProject.getLocalParent();
        }

        return scm;

    }

    public void addJavaVersion(ObjectNode extObject) {
        ObjectNode metadataNode = getMetadataNode(extObject);
        // Ignore if already set
        if (!metadataNode.has("minimum-java-version") && minimumJavaVersion != null) {
            metadataNode.put("minimum-java-version", minimumJavaVersion);
        }
    }

    private void addCapabilities(ObjectNode extObject) throws MojoExecutionException {
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

    private static ObjectNode getMetadataNode(ObjectNode extObject) {
        JsonNode mvalue = extObject.get(METADATA);
        ObjectNode metadata;
        if (mvalue != null && mvalue.isObject()) {
            metadata = (ObjectNode) mvalue;
        } else {
            metadata = extObject.putObject(METADATA);
        }
        return metadata;
    }

    private void validateExtensionDeps() throws MojoExecutionException {

        final ArtifactKey rootDeploymentGact = getDeploymentCoords().getKey();
        final RootNode rootDeployment = new RootNode(rootDeploymentGact, 2);
        final Artifact artifact = project.getArtifact();
        final Node rootRuntime = rootDeployment.newChild(ArtifactKey.of(artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getClassifier(), artifact.getType()), 1);

        rootDeployment.expectedDeploymentNodes.put(rootDeployment.gact, rootDeployment);
        rootDeployment.expectedDeploymentNodes.put(rootRuntime.gact, rootRuntime);
        // collect transitive extension deps
        final DependencyResult resolvedDeps;

        resolvedDeps = resolveRuntimeDeps();

        for (DependencyNode node : resolvedDeps.getRoot().getChildren()) {
            rootDeployment.directRuntimeDeps.add(toKey(node.getArtifact()));
        }
        visitRuntimeDeps(rootDeployment, rootDeployment, rootDeployment.id, resolvedDeps.getRoot());

        final DependencyNode deploymentNode = collectDeploymentDeps().getRoot();
        visitDeploymentDeps(rootDeployment, deploymentNode);

        if (rootDeployment.hasErrors()) {
            final Log log = getLog();
            log.error("Quarkus Extension Dependency Verification Error");

            final StringBuilder buf = new StringBuilder();

            if (rootDeployment.deploymentDepsTotal != 0) {
                log.error("Deployment artifact " + getDeploymentCoords() +
                        " was found to be missing dependencies on the Quarkus extension artifacts marked with '-' below:");
                final List<ArtifactKey> missing = rootDeployment.collectMissingDeploymentDeps(log);
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
                    log.error("The following deployment artifact(s) appear on the runtime classpath: ");
                    rootDeployment.collectDeploymentsOnRtCp(log);
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
                for (Map.Entry<ArtifactKey, org.eclipse.aether.artifact.Artifact> e : rootDeployment.unexpectedDeploymentDeps
                        .entrySet()) {
                    if (rootDeployment.allDeploymentDeps.contains(e.getKey())) {
                        unexpectedDeploymentDeps.add(e.getKey());
                    } else {
                        unexpectedRtDeps.add(toKey(e.getValue()));
                    }
                }

                if (!unexpectedRtDeps.isEmpty()) {
                    if (buf.length() > 0) {
                        buf.append(System.lineSeparator());
                    }
                    buf.append("The deployment artifact " + rootDeploymentGact
                            + " depends on the following Quarkus extension runtime artifacts that weren't found among the dependencies of "
                            + project.getArtifact() + ":");
                    for (ArtifactKey a : unexpectedRtDeps) {
                        buf.append(' ').append(a);
                    }
                    log.error("The deployment artifact " + rootDeploymentGact
                            + " depends on the following Quarkus extension runtime artifacts that weren't found among the dependencies of "
                            + project.getArtifact() + ":");
                    highlightInTree(deploymentNode, unexpectedRtDeps);
                }

                if (!unexpectedDeploymentDeps.isEmpty()) {
                    if (buf.length() > 0) {
                        buf.append(System.lineSeparator());
                    }
                    buf.append("The deployment artifact " + rootDeploymentGact
                            + " depends on the following Quarkus extension deployment artifacts whose corresponding runtime artifacts were not found among the dependencies of "
                            + project.getArtifact() + ":");
                    for (ArtifactKey a : unexpectedDeploymentDeps) {
                        buf.append(' ').append(a);
                    }
                    log.error("The deployment artifact " + rootDeploymentGact
                            + " depends on the following Quarkus extension deployment artifacts whose corresponding runtime artifacts were not found among the dependencies of "
                            + project.getArtifact() + ":");
                    highlightInTree(deploymentNode, unexpectedDeploymentDeps);
                }
            }

            throw new MojoExecutionException(buf.toString());
        }

    }

    private DependencyResult resolveRuntimeDeps() throws MojoExecutionException {
        if (runtimeDeps == null) {
            try {
                runtimeDeps = repoSystem.resolveDependencies(repoSession,
                        new DependencyRequest().setCollectRequest(newCollectRuntimeDepsRequest()));
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to resolve dependencies of " + project.getArtifact(), e);
            }
        }
        return runtimeDeps;
    }

    private void highlightInTree(DependencyNode node, Collection<ArtifactKey> keys) {
        highlightInTree(0, node, keys, new HashSet<>(), new StringBuilder(), new ArrayList<>());
    }

    private void highlightInTree(int depth, DependencyNode node, Collection<ArtifactKey> keysToHighlight,
            Set<ArtifactKey> visited, StringBuilder buf, List<String> branch) {
        final ArtifactKey key = toKey(node.getArtifact());
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
        for (int i = 0; i < depth; ++i) {
            buf.append("  ");
        }
        buf.append(node.getArtifact());
        branch.add(buf.toString());
        if (!highlighted) {
            for (DependencyNode child : node.getChildren()) {
                highlightInTree(depth + 1, child, keysToHighlight, visited, buf, branch);
            }
        } else {
            for (String line : branch) {
                getLog().error(line);
            }
        }
        branch.remove(branch.size() - 1);
    }

    private void visitDeploymentDeps(RootNode rootDeployment, DependencyNode dep) throws MojoExecutionException {
        for (DependencyNode child : dep.getChildren()) {
            visitDeploymentDep(rootDeployment, child);
        }
    }

    private void visitDeploymentDep(RootNode rootDeployment, DependencyNode dep) throws MojoExecutionException {
        org.eclipse.aether.artifact.Artifact artifact = dep.getArtifact();
        if (artifact == null) {
            return;
        }
        final ArtifactKey key = toKey(artifact);
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
            final ArtifactKey deployment = getDeploymentKey(artifact);
            if (deployment != null) {
                rootDeployment.unexpectedDeploymentDeps.put(deployment, artifact);
            }
        }
        visitDeploymentDeps(rootDeployment, dep);
    }

    private void visitRuntimeDep(RootNode root, Node currentNode, int currentId,
            DependencyNode node) throws MojoExecutionException {
        final org.eclipse.aether.artifact.Artifact a = node.getArtifact();
        root.allRtDeps.add(toKey(a));
        final ArtifactKey deployment = getDeploymentKey(a);
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

    private void visitRuntimeDeps(RootNode root, Node currentNode, int currentId, DependencyNode node)
            throws MojoExecutionException {
        for (DependencyNode child : node.getChildren()) {
            visitRuntimeDep(root, currentNode, currentId, child);
        }
    }

    private ArtifactKey getDeploymentKey(org.eclipse.aether.artifact.Artifact a) throws MojoExecutionException {
        final org.eclipse.aether.artifact.Artifact deployment = getDeploymentArtifact(a);
        return deployment == null ? null : toKey(deployment);
    }

    private org.eclipse.aether.artifact.Artifact getDeploymentArtifact(org.eclipse.aether.artifact.Artifact a)
            throws MojoExecutionException {
        final Properties props = getExtensionDescriptor(a, false);
        if (props == null) {
            return null;
        }
        final String deploymentStr = props.getProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT);
        if (deploymentStr == null) {
            throw new IllegalStateException("Quarkus extension runtime artifact " + a + " is missing "
                    + BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT + " property in its "
                    + BootstrapConstants.DESCRIPTOR_PATH);
        }
        return DependencyUtils.toArtifact(deploymentStr);
    }

    private Properties getExtensionDescriptor(org.eclipse.aether.artifact.Artifact a, boolean packaged) {
        final File f;
        try {
            f = resolve(a);
        } catch (Exception e) {
            getLog().warn("Failed to resolve " + a);
            return null;
        }
        // if it hasn't been packaged yet, we skip it, we are not packaging yet
        if (!a.getExtension().equals(ArtifactCoords.TYPE_JAR) || packaged && !isJarFile(f)) {
            return null;
        }
        try {
            if (f.isDirectory()) {
                return readExtensionDescriptorIfExists(f.toPath());
            }
            // In case of a parallel build, the resolved JAR might not have been fully written, which may result in a failure to read it
            // so we try the classes dir first
            if (session.isParallel()) {
                final LocalProject localProject = workspaceProvider.getProject(a.getGroupId(), a.getArtifactId());
                final Path classesDir = localProject == null ? null : localProject.getClassesDir();
                if (classesDir != null && Files.exists(classesDir)) {
                    return readExtensionDescriptorIfExists(classesDir);
                }
            }
            try (FileSystem fs = ZipUtils.newFileSystem(f.toPath())) {
                return readExtensionDescriptorIfExists(fs.getPath(""));
            }
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to read " + f, e);
        }
    }

    private Properties readExtensionDescriptorIfExists(final Path classesDir) throws IOException {
        final Path p = getExtensionDescriptorOrNull(classesDir);
        return p == null ? null : readExtensionDescriptor(p);
    }

    private Path getExtensionDescriptorOrNull(Path runtimeExtRootDir) {
        final Path p = runtimeExtRootDir.resolve(BootstrapConstants.DESCRIPTOR_PATH);
        return Files.exists(p) ? p : null;
    }

    private Properties readExtensionDescriptor(final Path extDescr) throws IOException {
        final Properties props = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(extDescr)) {
            props.load(reader);
        }
        return props;
    }

    private static ArtifactKey toKey(org.eclipse.aether.artifact.Artifact a) {
        return DependencyUtils.getKey(a);
    }

    private CollectResult collectDeploymentDeps() throws MojoExecutionException {
        if (collectedDeploymentDeps == null) {
            final ArtifactCoords deploymentCoords = getDeploymentCoords();
            try {
                collectedDeploymentDeps = repoSystem.collectDependencies(repoSession,
                        newCollectRequest(new DefaultArtifact(deploymentCoords.getGroupId(), deploymentCoords.getArtifactId(),
                                deploymentCoords.getClassifier(), deploymentCoords.getType(), deploymentCoords.getVersion())));
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to collect dependencies of deployment artifact " + deploymentCoords,
                        e);
            }
        }
        return collectedDeploymentDeps;
    }

    private ArtifactCoords getDeploymentCoords() {
        return deploymentCoords == null ? deploymentCoords = ArtifactCoords.fromString(deployment) : deploymentCoords;
    }

    private CollectRequest newCollectRuntimeDepsRequest() throws MojoExecutionException {
        return newCollectRequest(new DefaultArtifact(project.getArtifact().getGroupId(),
                project.getArtifact().getArtifactId(),
                project.getArtifact().getClassifier(),
                project.getArtifact().getArtifactHandler().getExtension(),
                project.getArtifact().getVersion()));
    }

    private CollectRequest newCollectRequest(DefaultArtifact projectArtifact) throws MojoExecutionException {
        final ArtifactDescriptorResult projectDescr;
        try {
            projectDescr = repoSystem.readArtifactDescriptor(repoSession,
                    new ArtifactDescriptorRequest()
                            .setArtifact(projectArtifact)
                            .setRepositories(repos));
        } catch (ArtifactDescriptorException e) {
            throw new MojoExecutionException("Failed to read descriptor of " + projectArtifact, e);
        }

        final CollectRequest request = new CollectRequest().setRootArtifact(projectArtifact)
                .setRepositories(repos)
                .setManagedDependencies(projectDescr.getManagedDependencies());
        for (Dependency dep : projectDescr.getDependencies()) {
            if ("test".equals(dep.getScope())
                    || "provided".equals(dep.getScope())
                    || dep.isOptional()) {
                continue;
            }
            request.addDependency(dep);
        }
        return request;
    }

    private boolean isJarFile(final File f) {
        return f != null && f.getName().endsWith(".jar") && f.exists() && !f.isDirectory();
    }

    private void transformLegacyToNew(ObjectNode extObject, ObjectMapper mapper) {
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
            YAMLFactory yf = new YAMLFactory();
            return new ObjectMapper(yf)
                    .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
        } else {
            return JsonMapper.builder()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                    .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
                    .propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
                    .build();
        }
    }

    private static final class QuarkusCoreDeploymentVersionLocator implements DependencyVisitor {
        String coreVersion;
        private boolean skipTheRest;

        @Override
        public boolean visitEnter(DependencyNode dep) {
            if (skipTheRest) {
                return false;
            }
            org.eclipse.aether.artifact.Artifact artifact = dep.getArtifact();
            if (artifact != null && artifact.getArtifactId().equals("quarkus-core")) {
                coreVersion = artifact.getVersion();
                if ("io.quarkus".equals(artifact.getGroupId())) {
                    skipTheRest = true;
                }
            }
            return skipTheRest ? false : true;
        }

        @Override
        public boolean visitLeave(DependencyNode node) {
            return skipTheRest ? false : true;
        }
    }

    private static class RootNode extends Node {

        final Map<ArtifactKey, Node> expectedDeploymentNodes = new HashMap<>();
        final Set<ArtifactKey> directRuntimeDeps = new HashSet<>();
        final Set<ArtifactKey> allRtDeps = new HashSet<>();
        final Set<ArtifactKey> allDeploymentDeps = new HashSet<>();
        final Map<ArtifactKey, org.eclipse.aether.artifact.Artifact> unexpectedDeploymentDeps = new HashMap<>(0);

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

        List<ArtifactKey> collectMissingDeploymentDeps(Log log) {
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
                for (int i = 0; i < depth; ++i) {
                    buf.append("    ");
                }
                buf.append(n.gact);
                log1.error(buf.toString());
            });
            return missing;
        }

        List<ArtifactKey> collectDeploymentsOnRtCp(Log log) {
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
                for (int i = 0; i < depth; ++i) {
                    buf.append("    ");
                }
                buf.append(n.gact);
                log1.error(buf.toString());
            });
            return missing;
        }

        private void handle(Log log, int depth, List<ArtifactKey> collected, NodeHandler handler) {
            handler.handle(log, depth, this, collected);
            handleChildren(log, depth, collected, handler);
        }

        private void handleChildren(Log log, int depth, List<ArtifactKey> collected, NodeHandler handler) {
            for (Node child : children) {
                child.handle(log, depth + 1, collected, handler);
            }
        }
    }

    private static interface NodeHandler {
        void handle(Log log, int depth, Node n, List<ArtifactKey> collected);
    }

    private MavenArtifactResolver resolver() throws MojoExecutionException {
        if (resolver == null) {
            final DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(repoSession);
            session.setWorkspaceReader(workspaceProvider.workspace());
            try {
                final BootstrapMavenContext ctx = new BootstrapMavenContext(BootstrapMavenContext.config()
                        .setRepositorySystem(repoSystem)
                        .setRemoteRepositoryManager(remoteRepoManager)
                        .setRepositorySystemSession(session)
                        .setRemoteRepositories(repos)
                        .setPreferPomsFromWorkspace(true)
                        .setCurrentProject(workspaceProvider.origin()));
                resolver = new MavenArtifactResolver(ctx);
            } catch (BootstrapMavenException e) {
                throw new MojoExecutionException("Failed to initialize Maven artifact resolver", e);
            }
        }
        return resolver;
    }

    private File resolve(org.eclipse.aether.artifact.Artifact a) throws MojoExecutionException {
        try {
            return resolver().resolve(a).getArtifact().getFile();
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to resolve " + a, e);
        }
    }
}
