package io.quarkus.maven;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.DependencyRequest;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.maven.capabilities.CapabilitiesConfig;
import io.quarkus.maven.dependency.ArtifactCoords;

/**
 * Generates Quarkus extension descriptor for the runtime artifact.
 * <p>
 * <p/>
 * Also generates META-INF/quarkus-extension.json which includes properties of
 * the extension such as name, labels, maven coordinates, etc that are used by
 * the tools.
 * <p>
 * Delegates all logic to {@link ExtensionDescriptorGenerator}, adapting Maven
 * APIs to the generator's Maven-free interfaces.
 *
 * @author Alexey Loubyansky
 */
@Mojo(name = "extension-descriptor", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class ExtensionDescriptorMojo extends AbstractMojo {

    public static class RemovedResources {
        String key;
        String resources;
    }

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
     * <a href="https://quarkus.io/guides/conditional-extension-dependencies">Conditional dependencies</a> that should be
     * enabled in case an application is launched in dev mode and certain classpath conditions have been satisfied.
     */
    @Parameter
    private List<String> conditionalDevDependencies = new ArrayList<>(0);

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

    /**
     * The Quarkus core version range that this extension requires
     */
    @Parameter(property = "requiresQuarkusCore")
    String requiresQuarkusCore;

    /**
     * Extension Dev mode configuration options
     */
    @Parameter
    ExtensionDevModeMavenConfig devMode;

    MavenArtifactResolver resolver;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            ExtensionDescriptorGenerator generator = new ExtensionDescriptorGenerator.Builder()
                    .groupId(project.getGroupId())
                    .artifactId(project.getArtifactId())
                    .version(project.getVersion())
                    .projectName(project.getName())
                    .projectDescription(project.getDescription())
                    .deployment(deployment)
                    .capabilities(capabilities)
                    .conditionalDependencies(conditionalDependencies)
                    .conditionalDevDependencies(conditionalDevDependencies)
                    .dependencyCondition(dependencyCondition)
                    .excludedArtifacts(excludedArtifacts)
                    .removedResources(convertRemovedResources())
                    .parentFirstArtifacts(parentFirstArtifacts)
                    .runnerParentFirstArtifacts(runnerParentFirstArtifacts)
                    .lesserPriorityArtifacts(lesserPriorityArtifacts)
                    .devMode(convertDevMode())
                    .minimumJavaVersion(minimumJavaVersion)
                    .requiresQuarkusCore(requiresQuarkusCore)
                    .skipExtensionValidation(skipExtensionValidation)
                    .ignoreNotDetectedQuarkusCoreVersion(ignoreNotDetectedQuarkusCoreVersion)
                    .skipCodestartValidation(skipCodestartValidation)
                    .outputDirectory(outputDirectory.toPath())
                    .extensionFile(extensionFile != null ? extensionFile.toPath() : null)
                    .scmUrl(getScmUrl())
                    .modelDependencies(convertModelDependencies())
                    .resolver(createDependencyResolver())
                    .logger(createLogger())
                    .build();

            generator.generate();
        } catch (Exception e) {
            if (e instanceof MojoExecutionException) {
                throw (MojoExecutionException) e;
            }
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private String getScmUrl() {
        Scm scm = getScm();
        return scm != null ? scm.getUrl() : null;
    }

    private Scm getScm() {
        // We have three ways to do this; project.getScm() will query the derived model. Sadly, inherited <scm> entries are usually wrong, unless the parent is in the same project
        // We can use getOriginalModel and getParent to walk the tree, but this will miss parents in poms outside the current execution, which might include a local reactor that we'd actually want to query
        // Or we can use the bootstrap provider
        Scm scm = null;
        final Artifact artifact = project.getArtifact();
        LocalProject localProject = workspaceProvider.getProject(artifact.getGroupId(), artifact.getArtifactId());
        if (localProject == null) {
            getLog().debug("Workspace provider could not resolve local project for " + artifact.getGroupId() + ":"
                    + artifact.getArtifactId());
        }
        while (scm == null && localProject != null) {
            scm = localProject.getRawModel().getScm();
            localProject = localProject.getLocalParent();
        }
        return scm;
    }

    private List<ExtensionDescriptorGenerator.RemovedResourceEntry> convertRemovedResources() {
        List<ExtensionDescriptorGenerator.RemovedResourceEntry> result = new ArrayList<>();
        for (RemovedResources rr : removedResources) {
            result.add(new ExtensionDescriptorGenerator.RemovedResourceEntry(rr.key, rr.resources));
        }
        return result;
    }

    private ExtensionDescriptorGenerator.DevModeConfig convertDevMode() {
        if (devMode == null) {
            return null;
        }
        return new ExtensionDescriptorGenerator.DevModeConfig(
                devMode.getJvmOptions(),
                devMode.getXxJvmOptions(),
                devMode.getLockJvmOptions(),
                devMode.getLockXxJvmOptions());
    }

    private List<ExtensionDescriptorGenerator.ModelDependency> convertModelDependencies() {
        List<ExtensionDescriptorGenerator.ModelDependency> result = new ArrayList<>();
        for (org.apache.maven.model.Dependency d : project.getDependencies()) {
            result.add(new ExtensionDescriptorGenerator.ModelDependency(
                    d.getGroupId(), d.getArtifactId(), d.getClassifier(),
                    d.getType(), d.getVersion(), d.getScope(), d.isOptional()));
        }
        return result;
    }

    private ExtensionDescriptorGenerator.Logger createLogger() {
        final Log log = getLog();
        return new ExtensionDescriptorGenerator.Logger() {
            @Override
            public void debug(String msg) {
                log.debug(msg);
            }

            @Override
            public void warn(String msg) {
                log.warn(msg);
            }

            @Override
            public void error(String msg) {
                log.error(msg);
            }
        };
    }

    private ExtensionDescriptorGenerator.DependencyResolver createDependencyResolver() {
        return new ExtensionDescriptorGenerator.DependencyResolver() {
            @Override
            public ExtensionDescriptorGenerator.DepNode resolveRuntimeDependencies() throws Exception {
                return toDepNode(repoSystem.resolveDependencies(repoSession,
                        new DependencyRequest().setCollectRequest(newCollectRuntimeDepsRequest())).getRoot());
            }

            @Override
            public ExtensionDescriptorGenerator.DepNode collectDeploymentDependencies(ArtifactCoords coords)
                    throws Exception {
                return toDepNode(repoSystem.collectDependencies(repoSession,
                        newCollectRequest(new DefaultArtifact(coords.getGroupId(), coords.getArtifactId(),
                                coords.getClassifier(), coords.getType(), coords.getVersion())))
                        .getRoot());
            }

            @Override
            public Path resolveArtifact(String groupId, String artifactId,
                    String classifier, String type, String version) throws Exception {
                File f = resolve(new DefaultArtifact(groupId, artifactId, classifier, type, version));
                return f != null ? f.toPath() : null;
            }

            @Override
            public boolean isInWorkspace(String groupId, String artifactId) {
                return workspaceProvider.getProject(groupId, artifactId) != null;
            }

            @Override
            public Path workspaceClassesDir(String groupId, String artifactId) {
                LocalProject lp = workspaceProvider.getProject(groupId, artifactId);
                return lp != null ? lp.getClassesDir() : null;
            }

            @Override
            public boolean isParallelBuild() {
                return session.isParallel();
            }

            @Override
            public boolean isAttachedArtifact(ArtifactCoords coords) {
                for (Artifact attached : project.getAttachedArtifacts()) {
                    if (coords.getArtifactId().equals(attached.getArtifactId()) &&
                            coords.getClassifier().equals(attached.getClassifier()) &&
                            coords.getType().equals(attached.getType()) &&
                            coords.getVersion().equals(attached.getVersion()) &&
                            coords.getGroupId().equals(attached.getGroupId())) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private ExtensionDescriptorGenerator.DepNode toDepNode(DependencyNode node) {
        org.eclipse.aether.artifact.Artifact a = node.getArtifact();
        List<ExtensionDescriptorGenerator.DepNode> children = new ArrayList<>();
        for (DependencyNode child : node.getChildren()) {
            children.add(toDepNode(child));
        }
        return new ExtensionDescriptorGenerator.DepNode(
                a != null ? a.getGroupId() : "",
                a != null ? a.getArtifactId() : "",
                a != null ? a.getClassifier() : "",
                a != null ? a.getExtension() : "jar",
                a != null ? a.getVersion() : "",
                a != null && a.getFile() != null ? a.getFile().toPath() : null,
                children);
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
