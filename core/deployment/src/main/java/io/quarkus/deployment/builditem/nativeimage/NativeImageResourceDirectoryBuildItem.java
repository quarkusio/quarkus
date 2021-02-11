package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that indicates that directory resources should be included in the native image
 * <p>
 * Related build items:
 * <ul>
 * <li>Use {@link NativeImageResourceBuildItem} if you need to add a single resource
 * <li>Use {@link NativeImageResourcePatternsBuildItem} to select resource paths by regular expressions or globs
 * </ul>
 */
public final class NativeImageResourceDirectoryBuildItem extends MultiBuildItem {

    private final String path;

    public NativeImageResourceDirectoryBuildItem(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
