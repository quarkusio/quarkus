package io.quarkus.rest.data.panache.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class RestDataResourceBuildItem extends MultiBuildItem {

    private final ResourceMetadata resourceMetadata;

    public RestDataResourceBuildItem(ResourceMetadata resourceMetadata) {
        this.resourceMetadata = resourceMetadata;
    }

    public ResourceMetadata getResourceMetadata() {
        return resourceMetadata;
    }
}
