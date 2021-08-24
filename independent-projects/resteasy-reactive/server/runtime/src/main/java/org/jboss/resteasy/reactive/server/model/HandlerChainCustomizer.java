package org.jboss.resteasy.reactive.server.model;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.server.spi.EndpointInvoker;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public interface HandlerChainCustomizer {

    /**
     *
     * @param phase The phase
     * @param resourceMethod The method, will be null if this has not been matched yet
     */
    default List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass, ServerResourceMethod resourceMethod) {
        return handlers(phase);
    }

    @Deprecated
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
         * handlers are invoked just after the resource method result has been turned into a {@link javax.ws.rs.core.Response}
         */
        AFTER_RESPONSE_CREATED,

    }
}
