package io.quarkus.vertx.http.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Marker class to turn on virtual http channel
 */
public final class RequireVirtualHttpBuildItem extends SimpleBuildItem {
    public static final RequireVirtualHttpBuildItem MARKER = new RequireVirtualHttpBuildItem();
}
