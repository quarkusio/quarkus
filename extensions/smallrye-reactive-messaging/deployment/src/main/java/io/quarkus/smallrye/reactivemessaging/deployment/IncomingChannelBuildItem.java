package io.quarkus.smallrye.reactivemessaging.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents an incoming channel from the application.
 * Basically, it represents a channel for which the application contains a {@code @Incoming} or {@code @Channel}.
 */
public final class IncomingChannelBuildItem extends MultiBuildItem {

    private final String name;
    private final String connector;

    IncomingChannelBuildItem(String name, String connector) {
        this.name = name;
        this.connector = connector;
    }

    /**
     * Creates a new instance of {@link IncomingChannelBuildItem}.
     *
     * @param name the name of the channel
     * @param connector the connector managing this channel if any.
     * @return the new {@link IncomingChannelBuildItem}
     */
    static IncomingChannelBuildItem of(String name, String connector) {
        return new IncomingChannelBuildItem(name, connector);
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
