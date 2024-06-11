package io.quarkus.maven;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import io.quarkus.maven.dependency.Dependency;
import io.quarkus.runtime.LaunchMode;

/**
 * Base class for Image related mojos.
 */
public class AbstractImageMojo extends BuildMojo {

    @Parameter(property = "quarkus.container-image.builder")
    String builderName;

    @Parameter(property = "quarkus.container-image.dry-run")
    boolean dryRun;

    public ImageBuilder getBuilder() {
        return ImageBuilder.getBuilder(builderName, mavenProject()).orElse(ImageBuilder.docker);
    }

    @Override
    protected boolean beforeExecute() throws MojoExecutionException {
        systemProperties.put("quarkus.container-image.builder", getBuilder().name());
        return super.beforeExecute();
    }

    @Override
    protected void doExecute() throws MojoExecutionException {
        if (dryRun) {
            getLog().info("Container image configuration:");
            systemProperties.entrySet().stream()
                    .filter(e -> e.getKey().contains("quarkus.container-image"))
                    .forEach(e -> getLog().info(" - " + e.getKey() + ": " + e.getValue()));
        } else {
            super.doExecute();
        }
    }

    @Override
    protected List<Dependency> forcedDependencies(LaunchMode mode) {
        List<Dependency> dependencies = new ArrayList<>();
        getBuilder().getExtensionArtifact(mavenProject()).ifPresent(dependencies::add);
        return dependencies;
    }
}
