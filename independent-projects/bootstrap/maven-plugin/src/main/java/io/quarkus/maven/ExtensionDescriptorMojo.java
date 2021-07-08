package io.quarkus.maven;

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
import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;
import io.quarkus.bootstrap.util.DependencyNodeUtils;
import io.quarkus.maven.capabilities.CapabilityConfig;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.maven.artifact.Artifact;
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

    private static final String GROUP_ID = "group-id";
    private static final String ARTIFACT_ID = "artifact-id";
    private static final String METADATA = "metadata";

    /**
     * The entry point to Aether, i.e. the component doing all the work.
     *
     * @component
     */
    @Component
    private RepositorySystem repoSystem;

    @Component
    RemoteRepositoryManager remoteRepoManager;

    @Component
    BootstrapWorkspaceProvider workspaceProvider;

    /**
     * The current repository/network configuration of Maven.
     *
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

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

    @Parameter(required = true, defaultValue = "${project.groupId}:${project.artifactId}-deployment:${project.version}")
    private String deployment;

    @Parameter(required = false)
    List<CapabilityConfig> capabilities = Collections.emptyList();

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

    @Parameter(required = false, defaultValue = "${skipExtensionValidation}")
    private boolean skipExtensionValidation;

    @Parameter(required = false, defaultValue = "${ignoreNotDetectedQuarkusCoreVersion")
    boolean ignoreNotDetectedQuarkusCoreVersion;

    @Parameter
    private List<String> conditionalDependencies = new ArrayList<>(0);

    @Parameter
    private List<String> dependencyCondition = new ArrayList<>(0);

    @Parameter(property = "skipCodestartValidation")
    boolean skipCodestartValidation;

    AppArtifactCoords deploymentCoords;
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
            buf.append(AppArtifactCoords.fromString(conditionalDependencies.get(i++)).toString());
            while (i < conditionalDependencies.size()) {
                buf.append(' ').append(AppArtifactCoords.fromString(conditionalDependencies.get(i++)).toString());
            }
            props.setProperty(BootstrapConstants.CONDITIONAL_DEPENDENCIES, buf.toString());
        }
        if (!dependencyCondition.isEmpty()) {
            final StringBuilder buf = new StringBuilder();
            int i = 0;
            buf.append(AppArtifactKey.fromString(dependencyCondition.get(i++)).toGacString());
            while (i < dependencyCondition.size()) {
                buf.append(' ').append(AppArtifactKey.fromString(dependencyCondition.get(i++)).toGacString());
            }
            props.setProperty(BootstrapConstants.DEPENDENCY_CONDITION, buf.toString());

        }

        if (!capabilities.isEmpty()) {
            final StringBuilder buf = new StringBuilder();
            appendCapability(capabilities.get(0), buf);
            for (int i = 1; i < capabilities.size(); ++i) {
                appendCapability(capabilities.get(i), buf.append(','));
            }
            props.setProperty(BootstrapConstants.PROP_PROVIDES_CAPABILITIES, buf.toString());
        }

        final Path output = outputDirectory.toPath().resolve(BootstrapConstants.META_INF);

        if (parentFirstArtifacts != null && !parentFirstArtifacts.isEmpty()) {
            String val = String.join(",", parentFirstArtifacts);
            props.put(AppModel.PARENT_FIRST_ARTIFACTS, val);
        }

        if (runnerParentFirstArtifacts != null && !runnerParentFirstArtifacts.isEmpty()) {
            String val = String.join(",", runnerParentFirstArtifacts);
            props.put(AppModel.RUNNER_PARENT_FIRST_ARTIFACTS, val);
        }

        if (excludedArtifacts != null && !excludedArtifacts.isEmpty()) {
            String val = String.join(",", excludedArtifacts);
            props.put(AppModel.EXCLUDED_ARTIFACTS, val);
        }

        if (lesserPriorityArtifacts != null && !lesserPriorityArtifacts.isEmpty()) {
            String val = String.join(",", lesserPriorityArtifacts);
            props.put(AppModel.LESSER_PRIORITY_ARTIFACTS, val);
        }

        try {
            Files.createDirectories(output);
            try (BufferedWriter writer = Files
                    .newBufferedWriter(output.resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME))) {
                props.store(writer, "Generated by extension-descriptor");
            }
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to persist extension descriptor " + output.resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME),
                    e);
        }

        // extension.json
        if (extensionFile == null) {
            extensionFile = new File(outputDirectory,
                    "META-INF" + File.separator + BootstrapConstants.QUARKUS_EXTENSION_FILE_NAME);
        }

        ObjectNode extObject;
        if (!extensionFile.exists()) {
            // if does not exist look for fallback .json
            extensionFile = new File(extensionFile.getParent(), "quarkus-extension.json");
        }

        ObjectMapper mapper = null;
        if (extensionFile.exists()) {
            mapper = getMapper(extensionFile.toString().endsWith(".yaml"));
            extObject = readJsonNode(extensionFile.toPath(), mapper);
        } else {
            mapper = getMapper(true);
            extObject = getMapper(true).createObjectNode();
        }

        transformLegacyToNew(output, extObject, mapper);

        JsonNode artifactNode = extObject.get("artifact");
        if (artifactNode == null) {
            final AppArtifactCoords coords = new AppArtifactCoords(
                    extObject.has("groupId") ? extObject.get("groupId").asText() : project.getGroupId(),
                    extObject.has("artifactId") ? extObject.get("artifactId").asText() : project.getArtifactId(),
                    null,
                    "jar",
                    extObject.has("version") ? extObject.get("version").asText() : project.getVersion());
            extObject.put("artifact", coords.toString());
        }

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
        addCapabilities(extObject);
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

    private ObjectNode readJsonNode(Path extensionFile, ObjectMapper mapper) throws MojoExecutionException {
        try {
            return readExtensionYaml(extensionFile, mapper);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to parse " + extensionFile, e);
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
        final AppArtifactCoords codestartArtifactCoords = AppArtifactCoords.fromString(codestartArtifact);
        codestartObject.put("artifact", codestartArtifactCoords.toString());
        if (!skipCodestartValidation) {
            // first we look for it in the workspace, if it's in there we don't need to actually resolve the artifact, because it might not have been built yet
            if (workspaceProvider.workspace().getProject(codestartArtifactCoords.getGroupId(),
                    codestartArtifactCoords.getArtifactId()) == null) {
                try {
                    resolve(new DefaultArtifact(codestartArtifact));
                } catch (MojoExecutionException e) {
                    throw new MojoExecutionException("Failed to resolve codestart artifact " + codestartArtifactCoords, e);
                }
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
            ObjectNode metadata;
            JsonNode mvalue = extObject.get(METADATA);
            if (mvalue != null && mvalue.isObject()) {
                metadata = (ObjectNode) mvalue;
            } else {
                metadata = extObject.putObject(METADATA);
            }
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
                        try (FileSystem fs = FileSystems.newFileSystem(p, (ClassLoader) null)) {
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
                        deps.add(new AppArtifactKey(a.getGroupId(), a.getArtifactId(), a.getClassifier(), a.getExtension())
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

    private void addCapabilities(ObjectNode extObject) throws MojoExecutionException {
        if (capabilities.isEmpty()) {
            return;
        }
        final ObjectNode capsNode = getMetadataNode(extObject).putObject("capabilities");
        final ArrayNode provides = capsNode.putArray("provides");
        for (CapabilityConfig cap : capabilities) {
            provides.add(cap.getName());
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

        final AppArtifactKey rootDeploymentGact = getDeploymentCoords().getKey();
        final RootNode rootDeployment = new RootNode(rootDeploymentGact, 2);
        final Artifact artifact = project.getArtifact();
        final Node rootRuntime = rootDeployment.newChild(new AppArtifactKey(artifact.getGroupId(), artifact.getArtifactId(),
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
                final List<AppArtifactKey> missing = rootDeployment.collectMissingDeploymentDeps(log);
                buf.append("Deployment artifact ");
                buf.append(getDeploymentCoords());
                buf.append(" is missing the following dependencies from its configuration: ");
                final Iterator<AppArtifactKey> i = missing.iterator();
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
                final Iterator<AppArtifactKey> i = rootDeployment.deploymentsOnRtCp.iterator();
                buf.append(i.next());
                while (i.hasNext()) {
                    buf.append(", ").append(i.next());
                }
            }

            if (!rootDeployment.unexpectedDeploymentDeps.isEmpty()) {
                final List<AppArtifactKey> unexpectedRtDeps = new ArrayList<>(0);
                final List<AppArtifactKey> unexpectedDeploymentDeps = new ArrayList<>(0);
                for (Map.Entry<AppArtifactKey, org.eclipse.aether.artifact.Artifact> e : rootDeployment.unexpectedDeploymentDeps
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
                    for (AppArtifactKey a : unexpectedRtDeps) {
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
                    for (AppArtifactKey a : unexpectedDeploymentDeps) {
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

    private void highlightInTree(DependencyNode node, Collection<AppArtifactKey> keys) {
        highlightInTree(0, node, keys, new HashSet<>(), new StringBuilder(), new ArrayList<>());
    }

    private void highlightInTree(int depth, DependencyNode node, Collection<AppArtifactKey> keysToHighlight,
            Set<AppArtifactKey> visited, StringBuilder buf, List<String> branch) {
        final AppArtifactKey key = toKey(node.getArtifact());
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
        final AppArtifactKey key = toKey(artifact);
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
            final AppArtifactKey deployment = getDeploymentKey(artifact);
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
        final AppArtifactKey deployment = getDeploymentKey(a);
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

    private AppArtifactKey getDeploymentKey(org.eclipse.aether.artifact.Artifact a) throws MojoExecutionException {
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
        return DependencyNodeUtils.toArtifact(deploymentStr);
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
        if (!a.getExtension().equals("jar") || packaged && !isJarFile(f)) {
            return null;
        }
        try {
            if (f.isDirectory()) {
                final Path p = getExtensionDescriptorOrNull(f.toPath());
                return p == null ? null : readExtensionDescriptor(p);
            } else {
                try (FileSystem fs = FileSystems.newFileSystem(f.toPath(), (ClassLoader) null)) {
                    final Path p = getExtensionDescriptorOrNull(fs.getPath(""));
                    return p == null ? null : readExtensionDescriptor(p);
                }
            }
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to read " + f, e);
        }
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

    private static AppArtifactKey toKey(org.eclipse.aether.artifact.Artifact a) {
        return DependencyNodeUtils.toKey(a);
    }

    private CollectResult collectDeploymentDeps() throws MojoExecutionException {
        if (collectedDeploymentDeps == null) {
            final AppArtifactCoords deploymentCoords = getDeploymentCoords();
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

    private AppArtifactCoords getDeploymentCoords() {
        return deploymentCoords == null ? deploymentCoords = AppArtifactCoords.fromString(deployment) : deploymentCoords;
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

    private void transformLegacyToNew(final Path output, ObjectNode extObject, ObjectMapper mapper)
            throws MojoExecutionException {
        ObjectNode metadata = null;

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

        JsonNode mvalue = extObject.get(METADATA);
        if (mvalue != null && mvalue.isObject()) {
            metadata = (ObjectNode) mvalue;
        } else {
            metadata = mapper.createObjectNode();
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

    /**
     * parse yaml or json and then return jackson JSonNode for furhter processing
     ***/
    private ObjectNode readExtensionYaml(Path descriptor, ObjectMapper mapper)
            throws IOException {
        try (InputStream is = Files.newInputStream(descriptor)) {
            return mapper.readValue(is, ObjectNode.class);
        } catch (IOException io) {
            throw new IOException("Failed to parse " + descriptor, io);
        }
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

        final Map<AppArtifactKey, Node> expectedDeploymentNodes = new HashMap<>();
        final Set<AppArtifactKey> directRuntimeDeps = new HashSet<>();
        final Set<AppArtifactKey> allRtDeps = new HashSet<>();
        final Set<AppArtifactKey> allDeploymentDeps = new HashSet<>();
        final Map<AppArtifactKey, org.eclipse.aether.artifact.Artifact> unexpectedDeploymentDeps = new HashMap<>(0);

        int deploymentDepsTotal = 1;
        List<AppArtifactKey> deploymentsOnRtCp = new ArrayList<>(0);

        RootNode(AppArtifactKey gact, int id) {
            super(null, gact, id);
        }

        boolean hasErrors() {
            return deploymentDepsTotal != 0 || runtimeCp != 0 || !unexpectedDeploymentDeps.isEmpty()
                    || !deploymentsOnRtCp.isEmpty();
        }
    }

    private static class Node {
        final Node parent;
        final AppArtifactKey gact;
        final int id;
        boolean present;
        int runtimeCp;
        List<Node> children = new ArrayList<>(0);

        Node(Node parent, AppArtifactKey gact, int id) {
            this.parent = parent;
            this.gact = gact;
            this.id = id;
        }

        Node newChild(AppArtifactKey gact, int id) {
            final Node child = new Node(this, gact, id);
            children.add(child);
            return child;
        }

        List<AppArtifactKey> collectMissingDeploymentDeps(Log log) {
            final List<AppArtifactKey> missing = new ArrayList<>();
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

        List<AppArtifactKey> collectDeploymentsOnRtCp(Log log) {
            final List<AppArtifactKey> missing = new ArrayList<>();
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

        private void handle(Log log, int depth, List<AppArtifactKey> collected, NodeHandler handler) {
            handler.handle(log, depth, this, collected);
            handleChildren(log, depth, collected, handler);
        }

        private void handleChildren(Log log, int depth, List<AppArtifactKey> collected, NodeHandler handler) {
            for (Node child : children) {
                child.handle(log, depth + 1, collected, handler);
            }
        }
    }

    private static interface NodeHandler {
        void handle(Log log, int depth, Node n, List<AppArtifactKey> collected);
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
