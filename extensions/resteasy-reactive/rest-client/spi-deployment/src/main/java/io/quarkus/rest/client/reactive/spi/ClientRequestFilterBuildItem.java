package io.quarkus.rest.client.reactive.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Used to register a global {@link jakarta.ws.rs.client.ClientRequestFilter}
 */
public final class ClientRequestFilterBuildItem extends MultiBuildItem {

    private final String className;

    public ClientRequestFilterBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
