package io.quarkus.rest.runtime.handlers;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;

public class AbortChainHandler implements ServerRestHandler {
    final ServerRestHandler[] abortChain;

    public AbortChainHandler(ServerRestHandler[] abortChain) {
        this.abortChain = abortChain;
    }

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        requestContext.setAbortHandlerChain(abortChain);
    }
}
