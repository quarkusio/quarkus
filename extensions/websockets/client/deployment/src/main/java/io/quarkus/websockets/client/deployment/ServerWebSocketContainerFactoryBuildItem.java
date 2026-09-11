package io.quarkus.websockets.client.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Marker build item indicating that the server module has registered a
 * {@link io.quarkus.websockets.client.runtime.ServerWebSocketContainerFactory}
 * service. When absent, the client module registers a default factory.
 */
public final class ServerWebSocketContainerFactoryBuildItem extends SimpleBuildItem {
}
