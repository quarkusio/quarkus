package io.quarkus.resteasy.server.common.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item that represents a path mapping from web.xml
 */
public final class ResteasyServletMappingBuildItem extends SimpleBuildItem {

    private final String path;

    public ResteasyServletMappingBuildItem(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
