package io.quarkus.vertx.http.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class WebsocketSubProtocolsBuildItem extends MultiBuildItem {

    private final String websocketSubProtocols;

    public WebsocketSubProtocolsBuildItem(String websocketSubProtocols) {
        this.websocketSubProtocols = websocketSubProtocols;
    }

    public String getWebsocketSubProtocols() {
        return websocketSubProtocols;
    }
}
