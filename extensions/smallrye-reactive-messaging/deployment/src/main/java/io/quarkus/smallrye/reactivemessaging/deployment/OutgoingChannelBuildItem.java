package io.quarkus.smallrye.reactivemessaging.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents an outgoing channel from the application.
 * Basically, it represents a channel for which the application contains a {@code @Outgoing} or
 * {@code @Channel Emitter|MutinyEmitter}.
 *
 */
public final class OutgoingChannelBuildItem extends MultiBuildItem {

    private final String name;
    private final String connector;

    public OutgoingChannelBuildItem(String name, String connector) {
        this.name = name;
        this.connector = connector;
    }

    /**
     * Creates a new instance of {@link OutgoingChannelBuildItem}.
     *
     * @param name the name of the channel
     * @param connector the connector managing this channel if any.
     * @return the new {@link OutgoingChannelBuildItem}
     */
    static OutgoingChannelBuildItem of(String name, String connector) {
        return new OutgoingChannelBuildItem(name, connector);
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
}
