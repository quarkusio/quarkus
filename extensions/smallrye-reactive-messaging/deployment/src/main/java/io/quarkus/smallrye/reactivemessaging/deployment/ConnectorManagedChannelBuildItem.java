package io.quarkus.smallrye.reactivemessaging.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a channel managed by a connector.
 */
public final class ConnectorManagedChannelBuildItem extends MultiBuildItem {

    private final String name;
    private final String connector;
    private final ConnectorBuildItem.Direction direction;

    public ConnectorManagedChannelBuildItem(String name, ConnectorBuildItem.Direction direction, String connector) {
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

    public ConnectorBuildItem.Direction getDirection() {
        return direction;
    }
}
