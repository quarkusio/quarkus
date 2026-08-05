package io.quarkus.vertx.http.deployment.spi;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Marker to indicate that the HTTP server has been initialized.
 */
public final class HttpServerStartedBuildItem extends SimpleBuildItem {
}
