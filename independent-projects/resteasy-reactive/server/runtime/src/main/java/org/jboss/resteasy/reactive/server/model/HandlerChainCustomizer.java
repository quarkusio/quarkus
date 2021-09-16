package org.jboss.resteasy.reactive.server.model;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.server.handlers.PublisherResponseHandler;
import org.jboss.resteasy.reactive.server.handlers.ResponseHandler;
import org.jboss.resteasy.reactive.server.spi.EndpointInvoker;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.resteasy.reactive.server.spi.StreamingResponse;

public interface HandlerChainCustomizer {

    /**
     *
     * @param phase The phase
     * @param resourceMethod The method, will be null if this has not been matched yet
     */
    default List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass, ServerResourceMethod resourceMethod) {
        return handlers(phase);
    }

    @Deprecated(forRemoval = true)
    default List<ServerRestHandler> handlers(Phase phase) {
        return Collections.emptyList();
    }

    /**
     * Returns an alternate invocation handler for this method.
     *
     * This is only considered for method level customizers
     * 
     * @param invoker
     */
    default ServerRestHandler alternateInvocationHandler(EndpointInvoker invoker) {
        return null;
    }

    /**
     * Returns an alternate endpoint invoker for this method.
     *
     * This is only considered for method level customizers
     * 
     * @param method
     */
    default Supplier<EndpointInvoker> alternateInvoker(ServerResourceMethod method) {
        return null;
    }

    /**
     * Returns a customizer for {@link ResponseBuilder}.
     * This will be used when the method invoker was called successfully and the result of the method was
     * not a {@link Response} or a {@link RestResponse}
     *
     * @param method
     */
    default ResponseHandler.ResponseBuilderCustomizer successfulInvocationResponseBuilderCustomizer(
            ServerResourceMethod method) {
        return null;
    }

    /**
     * Returns a customizer for {@link StreamingResponse}.
     * This will be used when a handler chain contains {@link PublisherResponseHandler} and the customizer
     * will be added to the list of customizers of that handler.
     *
     * @param method
     */
    default PublisherResponseHandler.StreamingResponseCustomizer streamingResponseCustomizer(
            ServerResourceMethod method) {
        return null;
    }

    enum Phase {
        /**
         * handlers are added right at the start of the pre match handler chain
         * this can only be applied globally, not on a per method basis
         */
        BEFORE_PRE_MATCH,
        /**
         * handlers are added at the end of the pre match handler chain, before
         * the next chain is determined and matched (i.e. after pre match filters)
         * this can only be applied globally, not on a per method basis
         */
        AFTER_PRE_MATCH,
        /**
         * handlers are invoked as soon as the matched handler chain is being run
         */
        AFTER_MATCH,
        /**
         * handlers are invoked as part of resolving method parameters
         */
        RESOLVE_METHOD_PARAMETERS,
        /**
         * handlers are invoked just before the resource method is invoked
         */
        BEFORE_METHOD_INVOKE,
        /**
         * handlers are invoked just after the resource method is invoked
         */
        AFTER_METHOD_INVOKE,
        /**
         * handlers are invoked just after the resource method result has been turned into a {@link Response}
         */
        AFTER_RESPONSE_CREATED,

    }
}
