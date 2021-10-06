package io.quarkus.smallrye.reactivemessaging.deployment;

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.reactive.messaging.annotations.ConnectorAttribute;

public final class ConnectorBuildItem extends MultiBuildItem {

    private final String name;
    private final Direction direction;
    private final List<ConnectorAttribute> attributes;

    private ConnectorBuildItem(String name, Direction direction, List<ConnectorAttribute> attributes) {
        this.name = name;
        this.direction = direction;
        this.attributes = attributes;
    }

    public static ConnectorBuildItem createInboundConnector(String name, List<ConnectorAttribute> attributes) {
        return new ConnectorBuildItem(name, Direction.INBOUND, attributes);
    }

    public static ConnectorBuildItem createOutboundConnector(String name, List<ConnectorAttribute> attributes) {
        return new ConnectorBuildItem(name, Direction.OUTBOUND, attributes);
    }

    public String getName() {
        return name;
    }

    public Direction getDirection() {
        return direction;
    }

    public List<ConnectorAttribute> getAttributes() {
        return attributes;
    }

    public enum Direction {
        INBOUND,
        OUTBOUND
    }

}
