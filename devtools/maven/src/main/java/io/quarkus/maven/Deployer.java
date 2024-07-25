package io.quarkus.maven;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

import io.quarkus.deployment.util.DeploymentUtil;
import io.quarkus.maven.dependency.ArtifactDependency;

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

    static final String QUARKUS_GROUP_ID = "io.quarkus";
    static final String QUARKUS_PREFIX = "quarkus-";

    /**
     * Get the {@link ArtifactDependency} matching the builder.
     *
     * @param project the target project
     * @return the dependency wrapped in {@link Optional}.
     */
    public Optional<ArtifactDependency> getExtensionArtifact(MavenProject project) {
        String artifactId = QUARKUS_PREFIX + name();
        return project.getDependencyManagement().getDependencies().stream()
                .filter(d -> QUARKUS_GROUP_ID.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId()))
                .map(d -> new ArtifactDependency(d.getGroupId(), d.getArtifactId(), null,
                        io.quarkus.maven.dependency.ArtifactCoords.TYPE_JAR,
                        d.getVersion()))
                .findFirst();
    }

    /**
     * Get the deployer by name or the first one found in the project.
     *
     * @param project the project to search for deployer extensions
     * @return the {@link Optional} builder matching the name, project.
     */
    public static Optional<Deployer> getDeployer(MavenProject project) {
        return DeploymentUtil.getEnabledDeployer()
                .or(() -> getProjectDeployer(project).stream().findFirst()).map(Deployer::valueOf);
    }

    /**
     * Get the deployer extensions found in the project.
     *
     * @param project The project to search for extensions
     * @return A set with the discovered extensions.
     */
    public static Set<String> getProjectDeployer(MavenProject project) {
        return getProjectDeployers(project.getDependencies());
    }

    /**
     * Get the deployer extensions found in the project.
     *
     * @param dependencies the dependencies for extensions
     * @return A set with the discovered extensions.
     */
    public static Set<String> getProjectDeployers(List<Dependency> dependencies) {
        if (dependencies == null) {
            return Set.of();
        }
        return dependencies.stream()
                .filter(d -> QUARKUS_GROUP_ID.equals(d.getGroupId()))
                .map(d -> strip(d.getArtifactId()))
                .filter(n -> Arrays.stream(Deployer.values()).anyMatch(e -> e.name().equals(n)))
                .collect(Collectors.toSet());
    }

    private static String strip(String s) {
        return s.replaceAll("^" + Pattern.quote(QUARKUS_PREFIX), "");
    }
}
