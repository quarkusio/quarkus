package org.jboss.resteasy.reactive.server.handlers;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public class AbortChainHandler implements ServerRestHandler {
    final ServerRestHandler[] abortChain;

    public AbortChainHandler(ServerRestHandler[] abortChain) {
        this.abortChain = abortChain;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        requestContext.setAbortHandlerChain(abortChain);
    }
}
