package io.quarkus.vertx.core.deployment;

import java.util.function.Supplier;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item that can be used to retrieve the number of events loops that have been configured/calculated
 */
public final class EventLoopCountBuildItem extends SimpleBuildItem {

    private final Supplier<Integer> eventLoopCount;

    public EventLoopCountBuildItem(Supplier<Integer> eventLoopCount) {
        this.eventLoopCount = eventLoopCount;
    }

    public Supplier<Integer> getEventLoopCount() {
        return eventLoopCount;
    }
}
