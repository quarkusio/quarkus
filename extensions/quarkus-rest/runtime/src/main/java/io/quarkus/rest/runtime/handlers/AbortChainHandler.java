package io.quarkus.rest.runtime.handlers;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;

public class AbortChainHandler implements RestHandler {
    final RestHandler[] abortChain;

    public AbortChainHandler(RestHandler[] abortChain) {
        this.abortChain = abortChain;
    }

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        requestContext.setAbortHandlerChain(abortChain);
    }
}
