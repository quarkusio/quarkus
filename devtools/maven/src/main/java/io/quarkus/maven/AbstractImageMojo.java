package io.quarkus.maven;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.runtime.LaunchMode;

/**
 * Base class for Image related mojos.
 */
public class AbstractImageMojo extends BuildMojo {

    public enum Builder {
        docker,
        jib,
        buildpack,
        openshift
    }

    @Parameter(defaultValue = "docker", property = "quarkus.container-image.builder")
    Builder builder = Builder.docker;

    @Parameter(property = "quarkus.container-image.dry-run")
    boolean dryRun;

    @Override
    protected boolean beforeExecute() throws MojoExecutionException {
        systemProperties.put("quarkus.container-image.builder", builder.name());
        return super.beforeExecute();
    }

    @Override
    protected void doExecute() throws MojoExecutionException {
        if (dryRun) {
            getLog().info("Container image confiugration:");
            systemProperties.entrySet().stream()
                    .filter(e -> e.getKey().contains("quarkus.container-image"))
                    .forEach(e -> {
                        getLog().info(" - " + e.getKey() + ": " + e.getValue());
                    });
        } else {
            super.doExecute();
        }
    }

    @Override
    protected List<Dependency> forcedDependencies(LaunchMode mode) {
        List<Dependency> dependencies = new ArrayList<>();
        getContainerImageExtension(builder).ifPresent(d -> dependencies.add(d));
        return dependencies;
    }

    protected Optional<ArtifactDependency> getContainerImageExtension(Builder builder) {
        return getExtension("quarkus-container-image-" + builder.name());
    }

    protected Optional<ArtifactDependency> getExtension(String artifactId) {
        return mavenProject().getDependencyManagement().getDependencies().stream()
                .filter(d -> "io.quarkus".equals(d.getGroupId()) && artifactId.equals(d.getArtifactId()))
                .map(d -> new ArtifactDependency(d.getGroupId(), d.getArtifactId(), null, ArtifactCoords.TYPE_JAR,
                        d.getVersion()))
                .findFirst();
    }

}
