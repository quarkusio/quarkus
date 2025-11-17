package io.quarkus.rest.client.reactive.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Used to register a global {@link jakarta.ws.rs.client.ClientResponseFilter}
 */
public final class ClientResponseFilterBuildItem extends MultiBuildItem {

    private final String className;

    public ClientResponseFilterBuildItem(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
