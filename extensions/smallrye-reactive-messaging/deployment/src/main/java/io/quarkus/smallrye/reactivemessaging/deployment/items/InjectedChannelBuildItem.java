package io.quarkus.smallrye.reactivemessaging.deployment.items;

import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.reactive.messaging.providers.extension.ChannelConfiguration;

/**
 * Represents a channel injection.
 */
public final class InjectedChannelBuildItem extends MultiBuildItem {

    /**
     * Creates a new instance of {@link InjectedChannelBuildItem}.
     *
     * @param name
     *        the name of the injected channel
     *
     * @return the new {@link InjectedChannelBuildItem}
     */
    public static InjectedChannelBuildItem of(String name) {
        return new InjectedChannelBuildItem(name);
    }

    /**
     * The name of the channel.
     */
    private final String name;

    public InjectedChannelBuildItem(String name) {
        this.name = name;
    }

    public ChannelConfiguration getChannelConfig() {
        return new ChannelConfiguration(name);
    }

}
