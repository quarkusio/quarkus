package io.quarkus.resteasy.reactive.server.spi;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Register a type as non-blocking by default when used as a return type of JAX-RS Resource
 */
public final class NonBlockingReturnTypeBuildItem extends MultiBuildItem {

    private final DotName type;

    public NonBlockingReturnTypeBuildItem(DotName type) {
        this.type = type;
    }

    public DotName getType() {
        return type;
    }
}
