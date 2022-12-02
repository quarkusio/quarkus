package io.quarkus.websockets.client.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.undertow.websockets.WebSocketDeploymentInfo;

public final class WebSocketDeploymentInfoBuildItem extends SimpleBuildItem {

    private final RuntimeValue<WebSocketDeploymentInfo> info;

    public WebSocketDeploymentInfoBuildItem(RuntimeValue<WebSocketDeploymentInfo> info) {
        this.info = info;
    }

    public RuntimeValue<WebSocketDeploymentInfo> getInfo() {
        return info;
    }
}
