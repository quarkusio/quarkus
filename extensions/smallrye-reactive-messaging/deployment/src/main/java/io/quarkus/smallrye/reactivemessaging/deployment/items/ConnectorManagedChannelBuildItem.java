package io.quarkus.smallrye.reactivemessaging.deployment.items;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a channel managed by a connector.
 */
public final class ConnectorManagedChannelBuildItem extends MultiBuildItem {

    private final String name;
    private final String connector;
    private final ChannelDirection direction;

    public ConnectorManagedChannelBuildItem(String name, ChannelDirection direction, String connector) {
        this.name = name;
        this.connector = connector;
        this.direction = direction;
    }

    public String getName() {
        return name;
    }

    public String getConnector() {
        return connector;
    }

    public ChannelDirection getDirection() {
        return direction;
    }
}
