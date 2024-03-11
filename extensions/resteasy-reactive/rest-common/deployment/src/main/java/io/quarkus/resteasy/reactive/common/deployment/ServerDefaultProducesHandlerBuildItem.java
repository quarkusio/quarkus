package io.quarkus.resteasy.reactive.common.deployment;

import org.jboss.resteasy.reactive.common.processor.DefaultProducesHandler;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Enable the registration of additional default produces handlers for server endpoints
 */
public final class ServerDefaultProducesHandlerBuildItem extends MultiBuildItem {

    private final DefaultProducesHandler defaultProducesHandler;

    public ServerDefaultProducesHandlerBuildItem(DefaultProducesHandler defaultProducesHandler) {
        this.defaultProducesHandler = defaultProducesHandler;
    }

    public DefaultProducesHandler getDefaultProducesHandler() {
        return defaultProducesHandler;
    }

    public static ServerDefaultProducesHandlerBuildItem json() {
        return new ServerDefaultProducesHandlerBuildItem(new JsonDefaultProducersHandler());
    }
}
