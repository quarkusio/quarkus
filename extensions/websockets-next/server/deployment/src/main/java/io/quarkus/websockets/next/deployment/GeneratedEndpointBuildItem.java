package io.quarkus.websockets.next.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A generated representation of a {@link io.quarkus.websockets.next.runtime.WebSocketEndpoint}.
 */
public final class GeneratedEndpointBuildItem extends MultiBuildItem {

    public final String endpointClassName;
    public final String generatedClassName;
    public final String path;

    GeneratedEndpointBuildItem(String endpointClassName, String generatedClassName, String path) {
        this.endpointClassName = endpointClassName;
        this.generatedClassName = generatedClassName;
        this.path = path;
    }

}
