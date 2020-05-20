package io.quarkus.rest.data.panache.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class RestDataResourceBuildItem extends MultiBuildItem {

    private final RestDataResourceInfo resourceInfo;

    public RestDataResourceBuildItem(RestDataResourceInfo resourceInfo) {
        this.resourceInfo = resourceInfo;
    }

    public RestDataResourceInfo getResourceInfo() {
        return resourceInfo;
    }
}
