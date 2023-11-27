package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.SimpleBuildItem;

public final class NativeImageAgentConfigDirectoryBuildItem extends SimpleBuildItem {
    private final String directory;

    public NativeImageAgentConfigDirectoryBuildItem(String directory) {
        this.directory = directory;
    }

    public String getDirectory() {
        return directory;
    }
}
