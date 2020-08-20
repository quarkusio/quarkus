package io.quarkus.rest.data.panache.deployment.properties;

import io.quarkus.builder.item.MultiBuildItem;

public class ResourcePropertiesBuildItem extends MultiBuildItem {

    private final String resourceType;

    private final ResourceProperties resourceProperties;

    public ResourcePropertiesBuildItem(String resourceType, ResourceProperties resourceProperties) {
        this.resourceType = resourceType;
        this.resourceProperties = resourceProperties;
    }

    public String getResourceType() {
        return resourceType;
    }

    public ResourceProperties getResourcePropertiesInfo() {
        return resourceProperties;
    }
}
