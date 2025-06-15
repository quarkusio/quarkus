package io.quarkus.netty.deployment;

import java.util.function.Supplier;

import io.netty.channel.EventLoopGroup;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Register EventLoopGroup suppliers to be used to produce main EventLoopGroup and boss EventLoopGroup annotated beans.
 * If not provided, both will be created from a default supplier. See EventLoopGroupBuildItem for actual supplier
 * instances
 */
public final class EventLoopSupplierBuildItem extends SimpleBuildItem {

    private final Supplier<EventLoopGroup> mainSupplier;
    private final Supplier<EventLoopGroup> bossSupplier;

    public EventLoopSupplierBuildItem(Supplier<EventLoopGroup> mainSupplier, Supplier<EventLoopGroup> bossSupplier) {
        this.mainSupplier = mainSupplier;
        this.bossSupplier = bossSupplier;
    }

    public Supplier<EventLoopGroup> getMainSupplier() {
        return mainSupplier;
    }

    public Supplier<EventLoopGroup> getBossSupplier() {
        return bossSupplier;
    }
}
