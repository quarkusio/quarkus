package io.quarkus.devui.spi;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Provides dev mode context as setup in Dev UI
 */
public final class DevContextBuildItem extends SimpleBuildItem {
    private final String devUIContextRoot;

    public DevContextBuildItem(String devUIContextRoot) {
        this.devUIContextRoot = devUIContextRoot;
    }

    public String getDevUIContextRoot() {
        return devUIContextRoot;
    }
}
