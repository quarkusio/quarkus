package io.quarkus.vertx.http.deployment.spi;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Marker build item indicating that the application uses a separate interface:port for the management endpoints such as
 * metrics, health and prometheus.
 */
public final class UseManagementInterfaceBuildItem extends SimpleBuildItem {
}
