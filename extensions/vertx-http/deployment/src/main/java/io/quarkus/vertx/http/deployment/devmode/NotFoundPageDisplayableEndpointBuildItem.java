package io.quarkus.vertx.http.deployment.devmode;

import io.quarkus.builder.item.MultiBuildItem;

final public class NotFoundPageDisplayableEndpointBuildItem extends MultiBuildItem {
    private final String endpoint;

    public NotFoundPageDisplayableEndpointBuildItem(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getEndpoint() {
        return endpoint;
    }
}
