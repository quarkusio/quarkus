package io.quarkus.kafka.client.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class DevServiceKafkaAdditionalBuildItem extends MultiBuildItem {
    private final String config;

    public DevServiceKafkaAdditionalBuildItem(String config) {
        this.config = config;
    }

    public String getConfig() {
        return config;
    }
}
