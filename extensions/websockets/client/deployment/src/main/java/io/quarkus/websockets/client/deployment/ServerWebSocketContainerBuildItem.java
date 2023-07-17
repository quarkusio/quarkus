package io.quarkus.websockets.client.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.undertow.websockets.ServerWebSocketContainer;

public final class ServerWebSocketContainerBuildItem extends SimpleBuildItem {

    private final RuntimeValue<ServerWebSocketContainer> container;

    public ServerWebSocketContainerBuildItem(RuntimeValue<ServerWebSocketContainer> container) {
        this.container = container;
    }

    public RuntimeValue<ServerWebSocketContainer> getContainer() {
        return container;
    }
}
