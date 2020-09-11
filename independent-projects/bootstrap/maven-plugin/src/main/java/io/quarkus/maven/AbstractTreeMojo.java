package io.quarkus.maven;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;

public class AbstractTreeMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Component
    RepositorySystem repoSystem;

    @Component
    RemoteRepositoryManager remoteRepoManager;

    @Parameter(defaultValue = "${project.remoteRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    protected MavenArtifactResolver resolver;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final AppArtifact appArtifact = new AppArtifact(project.getGroupId(), project.getArtifactId(), null, "pom",
                project.getVersion());
        final BootstrapAppModelResolver modelResolver;
        try {
            modelResolver = new BootstrapAppModelResolver(resolver());
            setupResolver(modelResolver);
            modelResolver.setBuildTreeLogger(s -> getLog().info(s));
            modelResolver.resolveModel(appArtifact);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to resolve application model " + appArtifact + " dependencies", e);
        }
    }

    protected MavenArtifactResolver resolver() throws BootstrapMavenException {
        return resolver == null
                ? resolver = MavenArtifactResolver.builder()
                        .setRepositorySystem(repoSystem)
                        .setRemoteRepositoryManager(remoteRepoManager)
                        //.setRepositorySystemSession(repoSession) the session should be initialized with the loaded workspace
                        .setRemoteRepositories(repos)
                        .build()
                : resolver;
    }

    protected void setupResolver(BootstrapAppModelResolver modelResolver) {
    }
}
