package org.jboss.resteasy.reactive.server.model;

import java.util.List;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public interface HandlerChainCustomizer {

    List<ServerRestHandler> handlers(Phase phase);

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

    }
}
