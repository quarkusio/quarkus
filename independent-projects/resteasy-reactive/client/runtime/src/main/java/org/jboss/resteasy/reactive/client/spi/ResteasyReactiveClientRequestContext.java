package org.jboss.resteasy.reactive.client.spi;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.GenericType;

import io.smallrye.stork.api.ServiceInstance;
import io.vertx.core.Context;

public interface ResteasyReactiveClientRequestContext extends ClientRequestContext {

    /**
     * The property used to store the (duplicated) vert.x context with the request.
     * This context is captured when the ResteasyReactiveClientRequestContext instance is created.
     * If, at that moment, there is no context, a new duplicated context is created.
     * If, we are executed on a root context, it creates a new duplicated context from it.
     * Otherwise, (we are already on a duplicated context), it captures it.
     */
    String VERTX_CONTEXT_PROPERTY = "__context";

    void suspend();

    void resume();

    void resume(Throwable t);

    /**
     * @return the captured or created duplicated context. See {@link #VERTX_CONTEXT_PROPERTY} for details.
     */
    Context getContext();

    GenericType<?> getResponseType();

    /***
     * This method is used to collect statistics about service instance calls in order to influence a Load Balancer algorithm
     * depending on these statistics.
     * You don't need to call this method in any case. It will be used by Stork when such a load balancer algorithm will be
     * configured.
     *
     * @param instance
     */
    void setCallStatsCollector(ServiceInstance instance);
}
