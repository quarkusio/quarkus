package io.quarkus.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.maven.components.BootstrapSessionListener;
import io.quarkus.maven.components.ManifestSection;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.runtime.LaunchMode;

public abstract class QuarkusBootstrapMojo extends AbstractMojo {

    static final String BOOTSTRAP_ID_PARAM = "bootstrapId";
    static final String CLOSE_BOOTSTRAPPED_APP_PARAM = "closeBootstrappedApp";
    static final String MODE_PARAM = "mode";
    static final String RELOAD_POMS_PARAM = "reloadPoms";

    static final String NATIVE_PROFILE_NAME = "native";

    @Component
    protected QuarkusBootstrapProvider bootstrapProvider;

    @Component(hint = "quarkus-bootstrap", role = AbstractMavenLifecycleParticipant.class)
    private BootstrapSessionListener bootstrapSessionListener;

    /**
     * The current repository/network configuration of Maven.
     *
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution of artifacts and their dependencies.
     *
     * @parameter default-value="${project.remoteProjectRepositories}"
     * @readonly
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Parameter(defaultValue = "${project.build.directory}")
    private File buildDir;

    @Parameter(defaultValue = "${project.build.finalName}")
    private String finalName;

    /**
     * The list of main manifest attributes
     */
    @Parameter
    private Map<String, String> manifestEntries = new LinkedHashMap<>();

    /**
     * The list of manifest sections
     */
    @Parameter
    private List<ManifestSection> manifestSections = new ArrayList<>();

    /**
     * When building an uber-jar, this array specifies entries that should
     * be excluded from the final jar. The entries are relative to the root of
     * the file. An example of this configuration could be:
     * <code><pre>
     * &#x3C;configuration&#x3E;
     *   &#x3C;uberJar&#x3E;true&#x3C;/uberJar&#x3E;
     *   &#x3C;ignoredEntries&#x3E;
     *     &#x3C;ignoredEntry&#x3E;META-INF/BC2048KE.SF&#x3C;/ignoredEntry&#x3E;
     *     &#x3C;ignoredEntry&#x3E;META-INF/BC2048KE.DSA&#x3C;/ignoredEntry&#x3E;
     *     &#x3C;ignoredEntry&#x3E;META-INF/BC1024KE.SF&#x3C;/ignoredEntry&#x3E;
     *     &#x3C;ignoredEntry&#x3E;META-INF/BC1024KE.DSA&#x3C;/ignoredEntry&#x3E;
     *   &#x3C;/ignoredEntries&#x3E;
     * &#x3C;/configuration&#x3E;
     * </pre></code>
     */
    @Parameter(property = "ignoredEntries")
    private String[] ignoredEntries;

    /**
     * Coordinates of the Maven artifact containing the original Java application to build the native image for.
     * If not provided, the current project is assumed to be the original Java application.
     * <p>
     * The coordinates are expected to be expressed in the following format:
     * <p>
     * groupId:artifactId:classifier:type:version
     * <p>
     * With the classifier, type and version being optional.
     * <p>
     * If the type is missing, the artifact is assumed to be of type JAR.
     * <p>
     * If the version is missing, the artifact is going to be looked up among the project dependencies using the provided
     * coordinates.
     *
     * <p>
     * However, if the expression consists of only three parts, it is assumed to be groupId:artifactId:version.
     *
     * <p>
     * If the expression consists of only four parts, it is assumed to be groupId:artifactId:classifier:type.
     */
    @Parameter(required = false, property = "appArtifact")
    private String appArtifact;

    /**
     * The properties of the plugin.
     */
    @Parameter(property = "properties", required = false)
    private Map<String, String> properties = new HashMap<>();

    /**
     * The context of the execution of the plugin.
     */
    @Parameter(defaultValue = "${mojoExecution}", readonly = true, required = true)
    MojoExecution mojoExecution;

    /**
     * Application bootstrap provider ID. This parameter is not supposed to be configured by the user.
     * To be able to re-use an application bootstrapped in one phase in a later phase, there needs to be a way
     * to identify the correct instance of the bootstrapped application (in case there are more than one) in each Mojo.
     * A bootstrap ID serves this purpose. This parameter is set in {@link DevMojo} invoking {@code generate-code}
     * and {@code generate-code-tests} goals. If this parameter is not configured, a Mojo execution ID will be used
     * as the bootstrap ID.
     */
    @Parameter(required = false)
    String bootstrapId;

    /**
     * Whether to close the bootstrapped applications after the execution
     */
    @Parameter(property = "quarkusCloseBootstrappedApp")
    Boolean closeBootstrappedApp;

    /**
     * POM files from the workspace that should be reloaded from the disk instead of taken from the Maven reactor.
     * This parameter is not supposed to be configured by a user.
     */
    @Parameter(property = "reloadPoms")
    Set<File> reloadPoms = Set.of();

    private ArtifactKey projectId;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!beforeExecute()) {
            return;
        }
        try {
            doExecute();
        } finally {
            if (closeBootstrappedApp != null) {
                // This trick is for dev mode from which we invoke other goals using the invoker API,
                // in which case the session listener won't be enabled and the app bootstrapped in generate-code will be closed immediately
                // causing DevMojo to bootstrap a new instance
                if (closeBootstrappedApp) {
                    bootstrapProvider.bootstrapper(this).close();
                }
            } else if (!bootstrapSessionListener.isEnabled()
                    // we may end up here when running 'compile quarkus:dev'
                    // even if the session listener is disabled we still don't want to close the provider
                    // because there might have been a Maven profile activated which we won't take into account
                    // when re-bootstrapping in DevMojo
                    && !mavenSession().getGoals().contains("quarkus:dev")) {
                bootstrapProvider.bootstrapper(this).close();
            }
        }
    }

    @Override
    public void setLog(Log log) {
        super.setLog(log);
        MojoLogger.delegate = log;
    }

    /**
     * This callback allows to evaluate whether this mojo should be executed, skipped or fail.
     *
     * @return false if the execution of the mojo should be skipped, true if the mojo should be executed
     * @throws MojoExecutionException in case of a failure
     * @throws MojoFailureException in case of a failure
     */
    protected abstract boolean beforeExecute() throws MojoExecutionException, MojoFailureException;

    /**
     * Main mojo execution code
     *
     * @throws MojoExecutionException in case of a failure
     * @throws MojoFailureException in case of a failure
     */
    protected abstract void doExecute() throws MojoExecutionException, MojoFailureException;

    protected String appArtifactCoords() {
        return appArtifact;
    }

    /**
     * Allows implementations to provide extra dependencies that should be enforced on the application.
     * Originally requested by Camel K.
     *
     * @param mode launch mode the application is being bootstrapped in
     * @return list of extra dependencies that should be enforced on the application
     */
    protected List<Dependency> forcedDependencies(LaunchMode mode) {
        return List.of();
    }

    @Deprecated(forRemoval = true)
    protected RepositorySystem repositorySystem() {
        return bootstrapProvider.repositorySystem();
    }

    @Deprecated(forRemoval = true)
    protected RemoteRepositoryManager remoteRepositoryManager() {
        return bootstrapProvider.remoteRepositoryManager();
    }

    protected RepositorySystemSession repositorySystemSession() {
        return repoSession;
    }

    protected List<RemoteRepository> remoteRepositories() {
        return repos;
    }

    protected MavenProject mavenProject() {
        return project;
    }

    public MavenSession mavenSession() {
        return session;
    }

    protected File buildDir() {
        return buildDir;
    }

    protected File baseDir() {
        return project.getBasedir();
    }

    protected String finalName() {
        return finalName;
    }

    protected Map<String, String> manifestEntries() {
        return manifestEntries;
    }

    protected List<ManifestSection> manifestSections() {
        return manifestSections;
    }

    protected String[] ignoredEntries() {
        return ignoredEntries;
    }

    protected Map<String, String> properties() {
        return properties;
    }

    protected String bootstrapId() {
        return bootstrapId == null ? mojoExecution.getExecutionId() : bootstrapId;
    }

    protected ArtifactKey projectId() {
        return projectId == null ? projectId = QuarkusBootstrapProvider.getProjectId(project) : projectId;
    }

    protected CuratedApplication bootstrapApplication() throws MojoExecutionException {
        return bootstrapApplication(LaunchMode.NORMAL);
    }

    protected CuratedApplication bootstrapApplication(LaunchMode mode) throws MojoExecutionException {
        return bootstrapProvider.bootstrapApplication(this, mode);
    }

    protected void closeApplication(LaunchMode mode) {
        bootstrapProvider.closeApplication(this, mode);
    }

    /**
     * Workspace ID associated with a given bootstrap mojo.
     * If the returned value is {@code 0}, a workspace was not associated with the bootstrap mojo.
     *
     * @return workspace ID associated with a given bootstrap mojo
     */
    protected int getWorkspaceId() {
        return bootstrapProvider.getWorkspaceId(this);
    }

    protected CuratedApplication bootstrapApplication(LaunchMode mode, Consumer<QuarkusBootstrap.Builder> builderCustomizer)
            throws MojoExecutionException {
        return bootstrapProvider.bootstrapApplication(this, mode, builderCustomizer);
    }

    protected Properties getBuildSystemProperties(boolean quarkusOnly) throws MojoExecutionException {
        return bootstrapProvider.bootstrapper(this).getBuildSystemProperties(this, quarkusOnly);
    }

    /**
     * Cause the native-enabled flag to be set if the native profile is enabled.
     *
     * @return true if the package type system property was set, otherwise - false
     */
    protected boolean setNativeEnabledIfNativeProfileEnabled() {
        if (!System.getProperties().containsKey("quarkus.native.enabled") && isNativeProfileEnabled(mavenProject())) {
            Object nativeEnabledProp = mavenProject().getProperties().get("quarkus.native.enabled");
            String nativeEnabled;
            if (nativeEnabledProp != null) {
                nativeEnabled = nativeEnabledProp.toString();
            } else {
                nativeEnabled = "true";
            }
            System.setProperty("quarkus.native.enabled", nativeEnabled);
            return true;
        } else {
            return false;
        }
    }

    static boolean isNativeProfileEnabled(MavenProject mavenProject) {
        // gotcha: mavenProject.getActiveProfiles() does not always contain all active profiles (sic!),
        //         but getInjectedProfileIds() does (which has to be "flattened" first)
        Stream<String> activeProfileIds = mavenProject.getInjectedProfileIds().values().stream().flatMap(List<String>::stream);
        if (activeProfileIds.anyMatch(NATIVE_PROFILE_NAME::equalsIgnoreCase)) {
            return true;
        }
        // recurse into parent (if available)
        return Optional.ofNullable(mavenProject.getParent()).map(QuarkusBootstrapMojo::isNativeProfileEnabled).orElse(false);
    }
}
