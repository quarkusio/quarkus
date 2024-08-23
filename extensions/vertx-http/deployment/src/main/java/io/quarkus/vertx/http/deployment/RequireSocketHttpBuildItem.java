package io.quarkus.vertx.http.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Marker class that can be used to force the socket to open even when using virtual HTTP.
 *
 * There are some use cases that may want to handle both real and virtual HTTP requests, such as mapping incoming
 * gRPC requests onto JAX-RS handlers.
 */
public final class RequireSocketHttpBuildItem extends SimpleBuildItem {
    public static final RequireSocketHttpBuildItem MARKER = new RequireSocketHttpBuildItem();
}
