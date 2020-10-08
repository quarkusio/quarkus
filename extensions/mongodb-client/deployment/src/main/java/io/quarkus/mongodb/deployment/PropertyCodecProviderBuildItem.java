package io.quarkus.mongodb.deployment;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

public final class PropertyCodecProviderBuildItem extends SimpleBuildItem {

    private List<String> propertyCodecProviderClassNames;

    public PropertyCodecProviderBuildItem(List<String> codecProviderClassNames) {
        this.propertyCodecProviderClassNames = codecProviderClassNames;
    }

    public List<String> getPropertyCodecProviderClassNames() {
        return propertyCodecProviderClassNames;
    }
}
