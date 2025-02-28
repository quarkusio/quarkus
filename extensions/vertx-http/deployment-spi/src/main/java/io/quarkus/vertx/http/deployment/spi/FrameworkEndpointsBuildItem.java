package io.quarkus.vertx.http.deployment.spi;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

public final class FrameworkEndpointsBuildItem extends SimpleBuildItem {
    private final Set<String> endpoints;

    public FrameworkEndpointsBuildItem(final Set<String> endpoints) {
        this.endpoints = endpoints;
    }

    public Set<String> getEndpoints() {
        return endpoints;
    }
}
