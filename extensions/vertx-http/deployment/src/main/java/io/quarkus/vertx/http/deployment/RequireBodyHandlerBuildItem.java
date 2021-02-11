package io.quarkus.vertx.http.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * This is a marker that indicates that the body handler should be installed
 * on all routes, as an extension requires the request to be fully buffered.
 */
public final class RequireBodyHandlerBuildItem extends MultiBuildItem {
}
