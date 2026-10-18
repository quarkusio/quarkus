package io.quarkus.websockets.client.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A server websocket endpoint
 */
public final class AnnotatedWebsocketEndpointBuildItem extends MultiBuildItem {

    final String className;
    final boolean client;

    public AnnotatedWebsocketEndpointBuildItem(String className, boolean client) {
        this.className = className;
        this.client = client;
    }

    public String getClassName() {
        return className;
    }

    public boolean isClient() {
        return client;
    }
}
