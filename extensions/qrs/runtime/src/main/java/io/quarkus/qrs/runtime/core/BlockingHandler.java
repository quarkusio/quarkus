package io.quarkus.qrs.runtime.core;

import java.util.concurrent.Executor;

public class BlockingHandler implements RestHandler {

    private final Executor executor;

    public BlockingHandler(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void handle(RequestContext requestContext) throws Exception {
        requestContext.suspend();
        requestContext.resume(executor);
    }
}
