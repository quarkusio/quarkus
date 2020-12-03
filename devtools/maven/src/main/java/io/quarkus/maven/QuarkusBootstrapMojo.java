package io.quarkus.maven;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
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
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;

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

    @Parameter(defaultValue = "${project.build.directory}")
    private File buildDir;

    @Parameter(defaultValue = "${project.build.finalName}")
    private String finalName;

    @Parameter(property = "uberJar", defaultValue = "false")
    private boolean uberJar;

    /**
     * When using the uberJar option, this array specifies entries that should
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

    private AppArtifactKey projectId;

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

    protected boolean uberJar() {
        return uberJar;
    }

    protected AppArtifactKey projectId() {
        return projectId == null ? projectId = new AppArtifactKey(project.getGroupId(), project.getArtifactId()) : projectId;
    }

    protected AppArtifact projectArtifact() throws MojoExecutionException {
        return bootstrapProvider.projectArtifact(this);
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
