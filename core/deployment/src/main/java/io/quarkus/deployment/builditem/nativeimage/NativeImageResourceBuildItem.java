package io.quarkus.deployment.builditem.nativeimage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that indicates that a static resource should be included in the native image.
 * <p>
 * A static resource is a file that is not processed by the build steps, but is included in the native image as-is.
 * The resource path passed to the constructor is a {@code /}-separated path name (with the same semantics as the parameters
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
