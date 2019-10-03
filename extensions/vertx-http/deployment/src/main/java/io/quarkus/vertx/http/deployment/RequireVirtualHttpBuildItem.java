package io.quarkus.vertx.http.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Marker class to turn on virtual http channel
 */
public final class RequireVirtualHttpBuildItem extends MultiBuildItem {
    public static final RequireVirtualHttpBuildItem MARKER = new RequireVirtualHttpBuildItem();
}
