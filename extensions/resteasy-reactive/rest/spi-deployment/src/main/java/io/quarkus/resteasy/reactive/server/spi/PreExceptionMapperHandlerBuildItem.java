package io.quarkus.resteasy.reactive.server.spi;

import jakarta.ws.rs.Priorities;

import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that allows extension to define a {@link ServerRestHandler} that runs write before
 * RESTEasy Reactive attempt to do exception mapping according to the JAX-RS spec.
 * This is only meant to be used in very advanced use cases.
 */
public final class PreExceptionMapperHandlerBuildItem extends MultiBuildItem
        implements Comparable<PreExceptionMapperHandlerBuildItem> {

    private final ServerRestHandler handler;
    private final int priority;

    public PreExceptionMapperHandlerBuildItem(ServerRestHandler handler, int priority) {
        this.handler = handler;
        this.priority = priority;
    }

    public PreExceptionMapperHandlerBuildItem(ServerRestHandler handler) {
        this.handler = handler;
        this.priority = Priorities.USER;
    }

    @Override
    public int compareTo(PreExceptionMapperHandlerBuildItem o) {
        return Integer.compare(priority, o.priority);
    }

    public ServerRestHandler getHandler() {
        return handler;
    }

    public int getPriority() {
        return priority;
    }
}
