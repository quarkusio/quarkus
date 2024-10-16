package io.quarkus.maven;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import io.quarkus.maven.dependency.Dependency;
import io.quarkus.runtime.LaunchMode;

public class AbstractDeploymentMojo extends BuildMojo {

    @Parameter(property = "quarkus.deployment.dry-run")
    boolean dryRun;

    @Parameter(property = "quarkus.container-image.build", defaultValue = "false")
    boolean imageBuild;

    @Parameter(property = "quarkus.container-image.builder")
    String imageBuilder;

    boolean forceDependencies = true;

    @Override
    protected void doExecute() throws MojoExecutionException {
        if (dryRun) {
            getLog().info("Deployment configuration:");
            systemProperties.entrySet().stream()
                    .filter(e -> e.getKey().contains("quarkus.deployment"))
                    .forEach(e -> getLog().info(" - " + e.getKey() + ": " + e.getValue()));
        } else {
            super.doExecute();
        }
    }

    public Deployer getDeployer() {
        return getDeployer(Deployer.kubernetes);
    }

    public Deployer getDeployer(Deployer defaultDeployer) {
        return Deployer.getDeployer(mavenProject())
                .orElse(defaultDeployer);
    }

    @Override
    protected List<Dependency> forcedDependencies(LaunchMode mode) {
        if (!forceDependencies)
            return super.forcedDependencies(mode);
        List<Dependency> dependencies = new ArrayList<>();
        MavenProject project = mavenProject();
        Deployer deployer = getDeployer();
        deployer.getExtensionArtifact(project).ifPresent(dependencies::add);
        if (this.imageBuild || this.imageBuilder != null) {
            Set<ImageBuilder> projectBuilders = ImageBuilder.getProjectBuilder(project).stream().map(ImageBuilder::valueOf)
                    .collect(Collectors.toSet());
            Optional<ImageBuilder> imageBuilder = ImageBuilder.getBuilder(this.imageBuilder, projectBuilders);
            imageBuilder.filter(b -> !projectBuilders.contains(b)).flatMap(b -> b.getExtensionArtifact(project))
                    .ifPresent(dependencies::add);
        }
        return dependencies;
    }
}
