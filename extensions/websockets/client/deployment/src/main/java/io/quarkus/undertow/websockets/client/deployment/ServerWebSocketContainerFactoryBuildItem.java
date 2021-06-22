package io.quarkus.undertow.websockets.client.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.undertow.websockets.client.runtime.ServerWebSocketContainerFactory;

public final class ServerWebSocketContainerFactoryBuildItem extends SimpleBuildItem {

    private final ServerWebSocketContainerFactory factory;

    public ServerWebSocketContainerFactoryBuildItem(ServerWebSocketContainerFactory factory) {
        this.factory = factory;
    }

    public ServerWebSocketContainerFactory getFactory() {
        return factory;
    }
}
