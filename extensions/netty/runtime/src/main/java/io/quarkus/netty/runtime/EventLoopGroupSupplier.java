package io.quarkus.netty.runtime;

import java.util.function.Supplier;

import io.netty.channel.EventLoopGroup;
import io.quarkus.runtime.annotations.RecordableConstructor;

/**
 * Supplies an event loop group that is created by a service, for build items that hand event loop groups to recorders.
 */
public final class EventLoopGroupSupplier implements Supplier<EventLoopGroup> {

    private final EventLoopGroup eventLoopGroup;

    @RecordableConstructor
    public EventLoopGroupSupplier(EventLoopGroup eventLoopGroup) {
        this.eventLoopGroup = eventLoopGroup;
    }

    public EventLoopGroup getEventLoopGroup() {
        return eventLoopGroup;
    }

    @Override
    public EventLoopGroup get() {
        return eventLoopGroup;
    }
}
