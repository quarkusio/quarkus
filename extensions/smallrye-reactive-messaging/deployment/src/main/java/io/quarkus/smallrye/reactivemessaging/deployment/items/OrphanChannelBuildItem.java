package io.quarkus.smallrye.reactivemessaging.deployment.items;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents an outbound channel without downstream or an inbound channel without upstream. In other words, this class
 * represents channels that should be managed by connectors.
 */
public final class OrphanChannelBuildItem extends MultiBuildItem {

    private final String name;
    private final ChannelDirection direction;

    public OrphanChannelBuildItem(ChannelDirection direction, String name) {
        this.direction = direction;
        this.name = name;
    }

    /**
     * Creates a new instance of {@link OrphanChannelBuildItem}.
     *
     * @param direction
     *        the direction of the channel
     * @param name
     *        the name of the channel
     *
     * @return the new {@link OrphanChannelBuildItem}
     */
    public static OrphanChannelBuildItem of(ChannelDirection direction, String name) {
        return new OrphanChannelBuildItem(direction, name);
    }

    public String getName() {
        return name;
    }

    public ChannelDirection getDirection() {
        return direction;
    }
}
