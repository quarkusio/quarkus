package io.quarkus.resteasy.reactive.server.deployment;

import org.jboss.resteasy.reactive.server.core.RequestContextFactory;

import io.quarkus.builder.item.SimpleBuildItem;

public final class RequestContextFactoryBuildItem extends SimpleBuildItem {

    private final RequestContextFactory factory;

    public RequestContextFactoryBuildItem(RequestContextFactory factory) {
        this.factory = factory;
    }

    public RequestContextFactory getFactory() {
        return factory;
    }
}
