package io.quarkus.maven;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import io.quarkus.deployment.util.DeploymentUtil;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.runtime.LaunchMode;

public class AbstractDeploymentMojo extends BuildMojo {

    public enum Deployer {

        kubernetes("quarkus-kubernetes", "quarkus-container-image-docker", "quarkus-container-image-jib",
                "quarkus-container-image-buildpack"),
        minikube("quarkus-minikube", "quarkus-container-image-docker", "quarkus-container-image-jib",
                "quarkus-container-image-buildpack"),
        kind("quarkus-kind", "quarkus-container-image-docker", "quarkus-container-image-jib",
                "quarkus-container-image-buildpack"),
        knative("quarkus-kubernetes", "quarkus-container-image-docker", "quarkus-container-image-jib",
                "quarkus-container-image-buildpack"),
        openshift("quarkus-openshift");

        private final String extension;
        private final String[] requiresOneOf;

        Deployer(String extension, String... requiresOneOf) {
            this.extension = extension;
            this.requiresOneOf = requiresOneOf;
        }

        public String getExtension() {
            return extension;
        }

        public String[] getRequiresOneOf() {
            return requiresOneOf;
        }
    }

    Deployer deployer = Deployer.kubernetes;

    @Parameter(property = "quarkus.deployment.dry-run")
    boolean dryRun;

    @Parameter(property = "quarkus.container-image.build", defaultValue = "false")
    boolean imageBuild;

    @Parameter(property = "quarkus.container-image.builder")
    String imageBuilder;

    @Override
    protected void doExecute() throws MojoExecutionException {
        if (dryRun) {
            getLog().info("Deployment configuration:");
            systemProperties.entrySet().stream()
                    .filter(e -> e.getKey().contains("quarkus.deployment"))
                    .forEach(e -> {
                        getLog().info(" - " + e.getKey() + ": " + e.getValue());
                    });
        } else {
            super.doExecute();
        }
    }

    public Optional<String> getImageBuilder() {
        return Optional.ofNullable(imageBuilder);
    }

    public Deployer getDeployer() {
        return DeploymentUtil.getEnabledDeployer().map(d -> Deployer.valueOf(d)).orElse(Deployer.kubernetes);
    }

    @Override
    protected List<Dependency> forcedDependencies(LaunchMode mode) {
        List<Dependency> dependencies = new ArrayList<>();
        Deployer deployer = getDeployer();
        String containerImageBuilderArtifactId = containerImageBuilderArtifactId(imageBuilder);
        getDeploymentExtension(deployer).ifPresent(d -> dependencies.add(d));
        getContainerImageExtension(containerImageBuilderArtifactId).or(() -> getFirstContainerImageExtension(deployer))
                .ifPresent(d -> dependencies.add(d));
        return dependencies;
    }

    protected Optional<ArtifactDependency> getDeploymentExtension(Deployer deployer) {
        return mavenProject().getDependencyManagement().getDependencies().stream()
                .filter(d -> "io.quarkus".equals(d.getGroupId()) && deployer.getExtension().equals(d.getArtifactId()))
                .map(d -> new ArtifactDependency(d.getGroupId(), d.getArtifactId(), null,
                        io.quarkus.maven.dependency.ArtifactCoords.TYPE_JAR,
                        d.getVersion()))
                .findFirst();
    }

    /**
     * Get the first {@link ArtifactDependency} that is requires by the specified {@link Deployer}.
     * The depndency is looked up in the project.
     *
     * @param deployer The deployer
     * @return a {@link Optional} containing the {@link ArtifactDependency} or empty if none is found.
     */
    protected Optional<ArtifactDependency> getFirstContainerImageExtension(Deployer deployer) {
        return getContainerImageExtension(Arrays.stream(deployer.requiresOneOf).findFirst());
    }

    /**
     * Get the first {@link ArtifactDependency} that matches the specified artifactId.
     * The depndency is looked up in the project (by artifactId).
     *
     * @param artifactId The artifactId to use for the lookup.
     * @return a {@link Optional} containing the {@link ArtifactDependency} or empty if none is found.
     */
    protected Optional<ArtifactDependency> getContainerImageExtension(String artifactId) {
        return getContainerImageExtension(Optional.ofNullable(artifactId));
    }

    /**
     * Get the first {@link ArtifactDependency} that matches the specified artifactId.
     * The depndency is looked up in the project (by artifactId).
     *
     * @param artifactId the {@link Optional} artifactId to use for the lookup.
     * @return a {@link Optional} containing the {@link ArtifactDependency} or empty if none is found.
     */
    protected Optional<ArtifactDependency> getContainerImageExtension(Optional<String> artifactId) {
        return artifactId.flatMap(a -> {
            return mavenProject().getDependencyManagement().getDependencies().stream()
                    .filter(d -> "io.quarkus".equals(d.getGroupId()) && a.equals(d.getArtifactId()))
                    .map(d -> new ArtifactDependency(d.getGroupId(), d.getArtifactId(), null,
                            io.quarkus.maven.dependency.ArtifactCoords.TYPE_JAR,
                            d.getVersion()))
                    .findFirst();

        });
    }

    protected static String containerImageBuilderArtifactId(String builder) {
        if (builder == null || builder.isEmpty()) {
            return null;
        }
        return "quarkus-container-image-" + builder;
    }
}
