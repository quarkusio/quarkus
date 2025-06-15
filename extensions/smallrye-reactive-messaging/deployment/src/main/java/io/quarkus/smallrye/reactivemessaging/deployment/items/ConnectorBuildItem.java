package io.quarkus.smallrye.reactivemessaging.deployment.items;

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.reactive.messaging.annotations.ConnectorAttribute;

/**
 * Represents a reactive messaging connector. It contains the name (like {@code smallrye-kafka}), the direction
 * (incoming or outgoing), and the list of connector attributes (mainly for documentation and tooling purpose).
 */
public final class ConnectorBuildItem extends MultiBuildItem {

    private final String name;
    private final ChannelDirection direction;
    private final List<ConnectorAttribute> attributes;

    ConnectorBuildItem(String name, ChannelDirection direction, List<ConnectorAttribute> attributes) {
        this.name = name;
        this.direction = direction;
        this.attributes = attributes;
    }

    public static ConnectorBuildItem createIncomingConnector(String name, List<ConnectorAttribute> attributes) {
        return new ConnectorBuildItem(name, ChannelDirection.INCOMING, attributes);
    }

    public static ConnectorBuildItem createOutgoingConnector(String name, List<ConnectorAttribute> attributes) {
        return new ConnectorBuildItem(name, ChannelDirection.OUTGOING, attributes);
    }

    public String getName() {
        return name;
    }

    public ChannelDirection getDirection() {
        return direction;
    }

    public List<ConnectorAttribute> getAttributes() {
        return attributes;
    }

}
