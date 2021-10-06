package io.quarkus.smallrye.reactivemessaging.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents an outbound channel without downstream or an inbound channel without upstream.
 * In other words, this class represents channels that should be managed by connectors.
 */
public final class OrphanChannelBuildItem extends MultiBuildItem {

    private final String name;
    private final ConnectorBuildItem.Direction direction;

    public OrphanChannelBuildItem(ConnectorBuildItem.Direction direction, String name) {
        this.direction = direction;
        this.name = name;
    }

    /**
     * Creates a new instance of {@link OrphanChannelBuildItem}.
     *
     * @param direction the direction of the channel
     * @param name the name of the channel
     * @return the new {@link OrphanChannelBuildItem}
     */
    static OrphanChannelBuildItem of(ConnectorBuildItem.Direction direction, String name) {
        return new OrphanChannelBuildItem(direction, name);
    }

    public String getName() {
        return name;
    }

    public ConnectorBuildItem.Direction getDirection() {
        return direction;
    }
}
