package io.quarkus.infinispan.client.deployment;

import java.util.Map;
import java.util.Properties;

import io.quarkus.builder.item.SimpleBuildItem;

public final class InfinispanPropertiesBuildItem extends SimpleBuildItem {

    private final Map<String, Properties> properties;

    public InfinispanPropertiesBuildItem(Map<String, Properties> properties) {
        this.properties = properties;
    }

    public Map<String, Properties> getProperties() {
        return properties;
    }
}
