package io.quarkus.mongodb.deployment;

import java.util.List;

import org.bson.codecs.pojo.PropertyCodecProvider;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Register additional {@link PropertyCodecProvider}s for the MongoDB clients.
 */
public final class PropertyCodecProviderBuildItem extends SimpleBuildItem {

    private final List<String> propertyCodecProviderClassNames;

    public PropertyCodecProviderBuildItem(List<String> codecProviderClassNames) {
        this.propertyCodecProviderClassNames = codecProviderClassNames;
    }

    public List<String> getPropertyCodecProviderClassNames() {
        return propertyCodecProviderClassNames;
    }
}
