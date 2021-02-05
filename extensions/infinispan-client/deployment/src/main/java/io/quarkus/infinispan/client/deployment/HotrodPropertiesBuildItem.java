package io.quarkus.infinispan.client.deployment;

import java.util.Properties;

import io.quarkus.builder.item.MultiBuildItem;

public class HotrodPropertiesBuildItem extends MultiBuildItem {
    /**
     * The highest precedence
     */
    public static int HIGHEST = Integer.MAX_VALUE;

    /**
     * The lowest precedence
     */
    public static int LOWEST = Integer.MIN_VALUE;

    private final Properties properties;
    private final int order;

    public HotrodPropertiesBuildItem(Properties properties) {
        this(properties, HIGHEST);
    }

    public HotrodPropertiesBuildItem(Properties properties, int order) {
        this.properties = properties;
        this.order = order;
    }

    public Properties getProperties() {
        return properties;
    }

    public int getOrder() {
        return order;
    }
}
