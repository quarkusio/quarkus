package io.quarkus.maven;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.AppModel;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;

/**
 * Generates Quarkus extension descriptor for the runtime artifact.
 *
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

    /**
     * The entry point to Aether, i.e. the component doing all the work.
     *
     * @component
     */
    @Component
    private RepositorySystem repoSystem;

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
     *
     * A possible example of this would be logging libraries, as these need to be loaded by the system class loader.
     */
    @Parameter
    List<String> parentFirstArtifacts;

    /**
     * Artifacts that are always loaded parent when the fast-jar is used. This is an advanced option
     * and should only be used if you are sure that this is the correct solution for the use case.
     *
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

    @Override
    public void execute() throws MojoExecutionException {

        if (!skipExtensionValidation) {
            validateExtensionDeps();
        }

        final Properties props = new Properties();
        props.setProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT, deployment);
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

            try {
                extObject = processPlatformArtifact(extensionFile.toPath(), mapper);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to parse " + extensionFile, e);
            }
        } else {
            mapper = getMapper(true);
            extObject = getMapper(true).createObjectNode();
        }

        transformLegacyToNew(output, extObject, mapper);

        if (extObject.get("groupId") == null) {
            extObject.put(GROUP_ID, project.getGroupId());
        }
        if (extObject.get("artifactId") == null) {
            extObject.put(ARTIFACT_ID, project.getArtifactId());
        }
        if (extObject.get("version") == null) {
            extObject.put("version", project.getVersion());
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

    private void validateExtensionDeps() throws MojoExecutionException {

        final AppArtifactCoords deploymentCoords = AppArtifactCoords.fromString(deployment);

        final AppArtifactKey rootDeploymentGact = deploymentCoords.getKey();
        final Node rootDeployment = new Node(null, rootDeploymentGact, 2);
        final Artifact artifact = project.getArtifact();
        final Node rootRuntime = rootDeployment.newChild(new AppArtifactKey(artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getClassifier(), artifact.getType()), 1);

        final Map<AppArtifactKey, Node> expectedExtensionDeps = new HashMap<>();
        expectedExtensionDeps.put(rootDeploymentGact, rootDeployment);
        expectedExtensionDeps.put(rootRuntime.gact, rootRuntime);
        // collect transitive extension deps
        final DependencyResult resolvedDeps;

        try {
            resolvedDeps = repoSystem.resolveDependencies(repoSession,
                    new DependencyRequest()
                            .setCollectRequest(newCollectRequest(new DefaultArtifact(project.getArtifact().getGroupId(),
                                    project.getArtifact().getArtifactId(),
                                    project.getArtifact().getClassifier(),
                                    project.getArtifact().getArtifactHandler().getExtension(),
                                    project.getArtifact().getVersion()))));
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to resolve dependencies of " + project.getArtifact(), e);
        }

        final AtomicInteger extDepsTotal = new AtomicInteger(2);
        resolvedDeps.getRoot().accept(new DependencyVisitor() {
            Node currentNode = rootDeployment;
            int currentNodeId = rootDeployment.id;

            @Override
            public boolean visitEnter(DependencyNode node) {
                ++currentNodeId;
                org.eclipse.aether.artifact.Artifact a = node.getArtifact();
                final File f = a.getFile();
                // if it hasn't been packaged yet, we skip it, we are not packaging yet
                if (isAnalyzable(f)) {
                    try (FileSystem fs = FileSystems.newFileSystem(f.toPath(), (ClassLoader) null)) {
                        final Path extDescr = fs.getPath(BootstrapConstants.DESCRIPTOR_PATH);
                        if (Files.exists(extDescr)) {
                            final Properties props = new Properties();
                            try (BufferedReader reader = Files.newBufferedReader(extDescr)) {
                                props.load(reader);
                            }
                            final String deploymentStr = props.getProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT);
                            if (deploymentStr == null) {
                                throw new IllegalStateException("Quarkus extension runtime artifact " + a + " is missing "
                                        + BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT + " property in its "
                                        + BootstrapConstants.DESCRIPTOR_PATH);
                            }
                            currentNode = currentNode.newChild(AppArtifactCoords.fromString(deploymentStr).getKey(),
                                    currentNodeId);
                            expectedExtensionDeps.put(currentNode.gact, currentNode);
                            extDepsTotal.incrementAndGet();
                        }
                    } catch (IOException e) {
                        throw new IllegalStateException("Failed to read " + f, e);
                    }
                }
                return true;
            }

            @Override
            public boolean visitLeave(DependencyNode node) {
                if (currentNodeId == currentNode.id && currentNode.parent != null) {
                    currentNode = currentNode.parent;
                }
                --currentNodeId;
                return true;
            }
        });

        final CollectResult collectedDeploymentDeps;
        try {
            collectedDeploymentDeps = repoSystem.collectDependencies(repoSession,
                    newCollectRequest(new DefaultArtifact(deploymentCoords.getGroupId(), deploymentCoords.getArtifactId(),
                            deploymentCoords.getClassifier(), deploymentCoords.getType(), deploymentCoords.getVersion())));
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to collect dependencies of deployment artifact " + deploymentCoords, e);
        }

        collectedDeploymentDeps.getRoot().accept(new DependencyVisitor() {
            @Override
            public boolean visitEnter(DependencyNode dep) {
                org.eclipse.aether.artifact.Artifact artifact = dep.getArtifact();
                if (artifact == null) {
                    return true;
                }
                final Node node = expectedExtensionDeps.get(new AppArtifactKey(artifact.getGroupId(), artifact.getArtifactId(),
                        artifact.getClassifier(), artifact.getExtension()));
                if (node != null && !node.included) {
                    node.included = true;
                    extDepsTotal.decrementAndGet();
                }
                return true;
            }

            @Override
            public boolean visitLeave(DependencyNode node) {
                return true;
            }
        });

        if (extDepsTotal.intValue() != 0) {
            final Log log = getLog();
            log.error("Quarkus Extension Dependency Verification Error");
            log.error("Deployment artifact " + deploymentCoords +
                    " was found to be missing dependencies on Quarkus extension artifacts marked with '-' below:");
            final List<AppArtifactKey> missing = rootDeployment.collectMissing(log);
            final StringBuilder buf = new StringBuilder();
            buf.append("Deployment artifact ");
            buf.append(deploymentCoords);
            buf.append(" is missing the following dependencies from its configuration: ");
            final Iterator<AppArtifactKey> i = missing.iterator();
            buf.append(i.next());
            while (i.hasNext()) {
                buf.append(", ").append(i.next());
            }
            throw new MojoExecutionException(buf.toString());
        }
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

    private boolean isAnalyzable(final File f) {
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

        JsonNode mvalue = extObject.get("metadata");
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

        extObject.set("metadata", metadata);

    }

    /**
     * parse yaml or json and then return jackson JSonNode for furhter processing
     *
     ***/
    private ObjectNode processPlatformArtifact(Path descriptor, ObjectMapper mapper)
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
                    .setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
        } else {
            return new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                    .enable(JsonParser.Feature.ALLOW_COMMENTS).enable(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS)
                    .setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
        }
    }

    private static class Node {
        final Node parent;
        final AppArtifactKey gact;
        final int id;
        boolean included;
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

        List<AppArtifactKey> collectMissing(Log log) {
            final List<AppArtifactKey> missing = new ArrayList<>();
            collectMissing(log, 0, missing);
            return missing;
        }

        private void collectMissing(Log log, int depth, List<AppArtifactKey> missing) {
            final StringBuilder buf = new StringBuilder();
            if (included) {
                buf.append('+');
            } else {
                buf.append('-');
                missing.add(gact);
            }
            buf.append(' ');
            for (int i = 0; i < depth; ++i) {
                buf.append("    ");
            }
            buf.append(gact);
            log.error(buf.toString());
            for (Node child : children) {
                child.collectMissing(log, depth + 1, missing);
            }
        }
    }
}
