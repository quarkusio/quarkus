package io.quarkus.vertx.http.deployment;

import io.quarkus.builder.item.EmptyBuildItem;

/**
 * Marker used by Build Steps that perform tasks which must run before the HTTP router has been finalized.
 */
public final class PreRouterFinalizationBuildItem extends EmptyBuildItem {
}
