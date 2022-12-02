package io.quarkus.resteasy.reactive.server.deployment;

import org.jboss.resteasy.reactive.server.model.ContextResolvers;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ContextResolversBuildItem extends SimpleBuildItem {

    private final ContextResolvers contextResolvers;

    public ContextResolversBuildItem(ContextResolvers contextResolvers) {
        this.contextResolvers = contextResolvers;
    }

    public ContextResolvers getContextResolvers() {
        return contextResolvers;
    }
}
