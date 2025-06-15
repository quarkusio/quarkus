package io.quarkus.smallrye.reactivemessaging.deployment.items;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents an application channel. These channels can be declared using {@code @Incoming}, {@code @Outgoing} or
 * injected {@code Emitter}, and {@code @Channel}.
 */
public final class ChannelBuildItem extends MultiBuildItem {

    private final String name;
    private final ChannelDirection direction;
    private final String connector;

    ChannelBuildItem(String name, ChannelDirection direction, String connector) {
        this.name = Objects.requireNonNull(name, "The channel name must be set");
        this.direction = Objects.requireNonNull(direction, "The direction of the channel must be set");
        this.connector = connector;
    }

    /**
     * Creates a new instance of {@link ChannelBuildItem} for an incoming channel.
     *
     * @param name
     *        the name of the channel
     * @param connector
     *        the connector managing this channel if any.
     *
     * @return the new {@link ChannelBuildItem}
     */
    public static ChannelBuildItem incoming(String name, String connector) {
        return new ChannelBuildItem(name, ChannelDirection.INCOMING, connector);
    }

    /**
     * Creates a new instance of {@link ChannelBuildItem} for an outgoing channel.
     *
     * @param name
     *        the name of the channel
     * @param connector
     *        the connector managing this channel if any.
     *
     * @return the new {@link ChannelBuildItem}
     */
    public static ChannelBuildItem outgoing(String name, String connector) {
        return new ChannelBuildItem(name, ChannelDirection.OUTGOING, connector);
    }

    public String getName() {
        return name;
    }

    public boolean isManagedByAConnector() {
        return connector != null;
    }

    public String getConnector() {
        return connector;
    }

    public ChannelDirection getDirection() {
        return direction;
    }
}
