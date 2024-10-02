package io.quarkus.maven;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.project.MavenProject;

import io.quarkus.maven.dependency.ArtifactDependency;

public enum ImageBuilder {

    docker,
    jib,
    buildpack,
    openshift;

    static final String QUARKUS_PREFIX = "quarkus-";
    static final String QUARKUS_CONTAINER_IMAGE_PREFIX = "quarkus-container-image-";

    /**
     * Get the {@link ArtifactDependency} matching the builder.
     *
     * @param project the target project
     * @return the dependency wrapped in {@link Optional}.
     */
    public Optional<ArtifactDependency> getExtensionArtifact(MavenProject project) {
        String artifactId = QUARKUS_CONTAINER_IMAGE_PREFIX + name();
        return project.getDependencyManagement().getDependencies().stream()
                .filter(d -> "io.quarkus".equals(d.getGroupId()) && artifactId.equals(d.getArtifactId()))
                .map(d -> new ArtifactDependency(d.getGroupId(), d.getArtifactId(), null,
                        io.quarkus.maven.dependency.ArtifactCoords.TYPE_JAR,
                        d.getVersion()))
                .findFirst();
    }

    /**
     * Get the image builder by name or the first one found in the project.
     *
     * @param name the name of the builder.
     * @project the project to search for container image builder extensions
     * @return the {@link Optional} builder matching the name, project.
     */
    public static Optional<ImageBuilder> getBuilder(String name, MavenProject project) {
        return Optional.ofNullable(name)
                .filter(n -> Arrays.stream(ImageBuilder.values()).map(ImageBuilder::name).anyMatch(i -> i.equals(n)))
                .or(() -> getProjectBuilder(project).stream().findFirst()).map(ImageBuilder::valueOf);
    }

    /**
     * Get the image builder by name or the first one found in the project.
     *
     * @param name the name of the builder.
     * @projectBuilder the collection of builders found in the project.
     * @return the {@link Optional} builder matching the name, project.
     */
    public static Optional<ImageBuilder> getBuilder(String name, Collection<ImageBuilder> projectBuilders) {
        return Optional.ofNullable(name)
                .filter(n -> Arrays.stream(ImageBuilder.values()).map(ImageBuilder::name).anyMatch(i -> i.equals(n)))
                .map(ImageBuilder::valueOf)
                .or(() -> projectBuilders.stream().findFirst());
    }

    /**
     * Get teh image builder extensions found in the project.
     *
     * @param the project to search for extensions
     * @return A set with the discovered extensions.
     */
    public static Set<String> getProjectBuilder(MavenProject project) {
        return project.getDependencies().stream()
                .filter(d -> "io.quarkus".equals(d.getGroupId()))
                .map(d -> strip(d.getArtifactId()))
                .filter(n -> Arrays.stream(ImageBuilder.values()).anyMatch(e -> e.equals(n)))
                .collect(Collectors.toSet());
    }

    private static final String strip(String s) {
        return s.replaceAll("^" + Pattern.quote(QUARKUS_CONTAINER_IMAGE_PREFIX), "")
                .replaceAll("^" + Pattern.quote(QUARKUS_PREFIX), "");
    }
}
