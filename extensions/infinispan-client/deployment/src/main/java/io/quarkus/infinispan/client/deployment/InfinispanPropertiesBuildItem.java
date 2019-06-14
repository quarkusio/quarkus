package io.quarkus.infinispan.client.deployment;

import java.util.Properties;

import io.quarkus.builder.item.SimpleBuildItem;

public final class InfinispanPropertiesBuildItem extends SimpleBuildItem {

    private final Properties properties;

    public InfinispanPropertiesBuildItem(Properties properties) {
        this.properties = properties;
    }

    public Properties getProperties() {
        return properties;
    }
}
