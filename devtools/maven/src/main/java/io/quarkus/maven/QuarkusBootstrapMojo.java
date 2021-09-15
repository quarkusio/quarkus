package io.quarkus.maven;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.maven.dependency.GACT;

public abstract class QuarkusBootstrapMojo extends AbstractMojo {

    @Component
    protected QuarkusBootstrapProvider bootstrapProvider;

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
    private MojoExecution mojoExecution;

    private GACT projectId;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!beforeExecute()) {
            return;
        }
        doExecute();
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

    protected RepositorySystem repositorySystem() {
        return bootstrapProvider.repositorySystem();
    }

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

    protected String[] ignoredEntries() {
        return ignoredEntries;
    }

    protected Map<String, String> properties() {
        return properties;
    }

    protected String executionId() {
        return mojoExecution.getExecutionId();
    }

    protected GACT projectId() {
        return projectId == null ? projectId = new GACT(project.getGroupId(), project.getArtifactId()) : projectId;
    }

    protected MavenArtifactResolver artifactResolver() throws MojoExecutionException {
        return bootstrapProvider.artifactResolver(this);
    }

    protected QuarkusBootstrap bootstrapQuarkus() throws MojoExecutionException {
        return bootstrapProvider.bootstrapQuarkus(this);
    }

    protected CuratedApplication bootstrapApplication() throws MojoExecutionException {
        return bootstrapProvider.bootstrapApplication(this);
    }
}
