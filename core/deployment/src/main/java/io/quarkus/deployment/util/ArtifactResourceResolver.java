package io.quarkus.deployment.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactCoordsPattern;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathFilter;
import io.quarkus.paths.PathVisit;
import io.quarkus.util.GlobUtil;

/**
 * Utility class to extract a list of resource paths from a given artifact and path.
 */
public final class ArtifactResourceResolver {
    private final Collection<ResolvedDependency> artifacts;

    /**
     * Creates a {@code ArtifactResourceResolver} for the given artifact
     *
     * @param dependencies the resolved dependencies of the build
     * @param artifactCoordinates the coordinates of the artifact containing the resources
     */
    public static ArtifactResourceResolver of(
            Collection<ResolvedDependency> dependencies, ArtifactCoords artifactCoordinates) {

        return new ArtifactResourceResolver(dependencies, List.of(artifactCoordinates));
    }

    /**
     * Creates a {@code ArtifactResourceResolver} for the given artifact
     *
     * @param dependencies the resolved dependencies of the build
     * @param artifactCoordinatesCollection a coordinates {@link Collection} for the artifacts containing the resources
     */
    public static ArtifactResourceResolver of(
            Collection<ResolvedDependency> dependencies, Collection<ArtifactCoords> artifactCoordinatesCollection) {

        return new ArtifactResourceResolver(dependencies, artifactCoordinatesCollection);
    }

    private ArtifactResourceResolver(
            Collection<ResolvedDependency> dependencies, Collection<ArtifactCoords> artifactCoordinates) {

        var patterns = ArtifactCoordsPattern.toPatterns(artifactCoordinates);
        artifacts = patterns.stream()
                .map(p -> findArtifact(dependencies, p))
                .collect(Collectors.toSet());
    }

    private static ResolvedDependency findArtifact(
            Collection<ResolvedDependency> dependencies, ArtifactCoordsPattern pattern) {

        return dependencies.stream()
                .filter(pattern::matches)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "%s artifact not found".formatted(pattern.toString())));
    }

    /**
     * Extracts a {@link Collection} of resource paths with the given filter
     *
     * @param pathFilter the filter for the resources in glob syntax (see {@link GlobUtil})
     * @return a collection of the found resource paths
     */
    public Collection<Path> resourcePathList(PathFilter pathFilter) {
        return artifacts.stream()
                .map(a -> pathsForArtifact(a, pathFilter))
                .flatMap(Collection::stream)
                .toList();
    }

    private Collection<Path> pathsForArtifact(ResolvedDependency artifact, PathFilter pathFilter) {
        var pathList = new ArrayList<Path>();
        var pathTree = artifact.getContentTree(pathFilter);
        pathTree.walk(visit -> pathList.add(relativePath(visit)));
        return pathList;
    }

    private Path relativePath(PathVisit visit) {
        var path = visit.getPath();
        return path.getRoot().relativize(path);
    }

    /**
     * Extracts a {@link List} of resource paths as strings with the given filter
     *
     * @param pathFilter the filter for the resources in glob syntax (see {@link GlobUtil})
     * @return a list of the found resource paths as strings
     */
    public List<String> resourceList(PathFilter pathFilter) {
        return resourcePathList(pathFilter).stream()
                .map(Path::toString)
                .toList();
    }
}
