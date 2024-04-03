package io.quarkus.websockets.next.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A generated representation of a {@link io.quarkus.websockets.next.runtime.WebSocketEndpoint}.
 */
public final class GeneratedEndpointBuildItem extends MultiBuildItem {

    public final String endpointId;
    public final String endpointClassName;
    public final String generatedClassName;
    public final String path;

    GeneratedEndpointBuildItem(String endpointId, String endpointClassName, String generatedClassName, String path) {
        this.endpointId = endpointId;
        this.endpointClassName = endpointClassName;
        this.generatedClassName = generatedClassName;
        this.path = path;
    }

}
