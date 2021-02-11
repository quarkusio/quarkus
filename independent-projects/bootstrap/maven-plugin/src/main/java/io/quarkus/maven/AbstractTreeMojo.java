package io.quarkus.maven;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public class AbstractTreeMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

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
        return resolver == null ? resolver = new MavenArtifactResolver(new BootstrapMavenContext()) : resolver;
    }

    protected void setupResolver(BootstrapAppModelResolver modelResolver) {
    }
}
