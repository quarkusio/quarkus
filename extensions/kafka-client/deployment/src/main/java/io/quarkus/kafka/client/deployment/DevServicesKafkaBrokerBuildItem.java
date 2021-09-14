package io.quarkus.kafka.client.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

public final class DevServicesKafkaBrokerBuildItem extends SimpleBuildItem {

    final String bootstrapServers;

    public DevServicesKafkaBrokerBuildItem(String bs) {
        this.bootstrapServers = bs;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

}
