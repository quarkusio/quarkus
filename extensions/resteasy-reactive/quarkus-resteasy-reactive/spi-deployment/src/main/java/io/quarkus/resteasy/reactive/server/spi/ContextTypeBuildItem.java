package io.quarkus.resteasy.reactive.server.spi;

import jakarta.ws.rs.core.Context;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Register types that should be available for injection into JAX-RS methods via {@link Context}
 */
public final class ContextTypeBuildItem extends MultiBuildItem {

    private final DotName type;

    public ContextTypeBuildItem(DotName className) {
        this.type = className;
    }

    public DotName getType() {
        return type;
    }
}
