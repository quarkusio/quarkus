package io.quarkus.vertx.http.deployment.devmode;

import io.quarkus.builder.item.MultiBuildItem;

final public class NotFoundPageDisplayableEndpointBuildItem extends MultiBuildItem {
    private final String endpoint;
    private final String description;

    public NotFoundPageDisplayableEndpointBuildItem(String endpoint, String description) {
        this.endpoint = endpoint;
        this.description = description;
    }

    public NotFoundPageDisplayableEndpointBuildItem(String endpoint) {
        this(endpoint, null);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getDescription() {
        return description;
    }
}
