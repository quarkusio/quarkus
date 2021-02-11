package io.quarkus.smallrye.reactivemessaging.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.reactive.messaging.extension.ChannelConfiguration;

public final class ChannelBuildItem extends MultiBuildItem {

    /**
     * Creates a new instance of {@link ChannelBuildItem}.
     *
     * @param name the name of the injected channel
     * @return the new {@link ChannelBuildItem}
     */
    static ChannelBuildItem of(String name) {
        return new ChannelBuildItem(name);
    }

    /**
     * The name of the channel.
     */
    private final String name;

    public ChannelBuildItem(String name) {
        this.name = name;
    }

    public ChannelConfiguration getChannelConfig() {
        return new ChannelConfiguration(name);
    }

}
