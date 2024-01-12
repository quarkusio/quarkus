package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Used when resources generated by the build should not end up in the produced runnable artifact,
 * but in the file system inside the output directory of OutputTargetBuildItem.
 *
 * This is written to the file system for normal and dev mode, but not for test mode.
 */
public final class GeneratedFileSystemResourceBuildItem extends MultiBuildItem {
    final String name;
    final byte[] classData;

    public GeneratedFileSystemResourceBuildItem(String name, byte[] data) {
        this.name = name;
        this.classData = data;
    }

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return classData;
    }
}
