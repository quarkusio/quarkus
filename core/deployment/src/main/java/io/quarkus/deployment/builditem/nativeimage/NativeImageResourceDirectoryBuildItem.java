package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that indicates that directory resources should be included in the native image
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
