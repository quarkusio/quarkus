package io.quarkus.vertx.http.deployment.spi;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

public final class FrameworkEndpointsBuildItem extends SimpleBuildItem {
    private final List<String> endpoints;

    public FrameworkEndpointsBuildItem(final List<String> endpoints) {
        this.endpoints = endpoints;
    }

    public List<String> getEndpoints() {
        return endpoints;
    }
}
