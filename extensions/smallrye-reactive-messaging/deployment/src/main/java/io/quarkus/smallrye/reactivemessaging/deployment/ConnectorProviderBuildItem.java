package io.quarkus.smallrye.reactivemessaging.deployment;

import io.quarkus.builder.item.MultiBuildItem;

public final class ConnectorProviderBuildItem extends MultiBuildItem {

    final String name;

    public ConnectorProviderBuildItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
