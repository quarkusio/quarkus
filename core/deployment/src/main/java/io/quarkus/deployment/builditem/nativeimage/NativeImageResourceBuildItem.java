package io.quarkus.deployment.builditem.nativeimage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.util.ArtifactResourceResolver;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathFilter;
import io.quarkus.util.GlobUtil;

/**
 * A build item that indicates that a static resource should be included in the native image.
 * <p>
 * A static resource is a file that is not processed by the build steps, but is included in the native image as-is.
 * The resource path passed to the constructor is a {@code /}-separated path name (with the same semantics as the parameters)
 * passed to {@link java.lang.ClassLoader#getResources(String)}.
 * <p>
 * Related build items:
 * <ul>
 * <li>Use {@link NativeImageResourceDirectoryBuildItem} if you need to add a directory of resources
 * <li>Use {@link NativeImageResourcePatternsBuildItem} to select resource paths by regular expressions or globs
 * </ul>
 */
public final class NativeImageResourceBuildItem extends MultiBuildItem {

    private final List<String> resources;

    /**
     * Builds a {@code NativeImageResourceBuildItem} for the given artifact and path
     *
     * @param dependencies the resolved dependencies of the build
     * @param artifactCoordinates the coordinates of the artifact containing the resources
     * @param resourceFilter the filter for the resources in glob syntax (see {@link GlobUtil})
     * @return
     */
    public static NativeImageResourceBuildItem ofDependencyResources(
            Collection<ResolvedDependency> dependencies,
            ArtifactCoords artifactCoordinates,
            PathFilter resourceFilter) {

        var resolver = ArtifactResourceResolver.of(dependencies, artifactCoordinates);
        return new NativeImageResourceBuildItem(resolver.resourceList(resourceFilter));
    }

    public NativeImageResourceBuildItem(String... resources) {
        this.resources = Arrays.asList(resources);
    }

    public NativeImageResourceBuildItem(List<String> resources) {
        this.resources = new ArrayList<>(resources);
    }

    public List<String> getResources() {
        return resources;
    }
}
