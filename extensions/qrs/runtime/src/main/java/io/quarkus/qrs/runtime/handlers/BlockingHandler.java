package io.quarkus.qrs.runtime.handlers;

import java.util.concurrent.Executor;

import io.quarkus.qrs.runtime.core.QrsRequestContext;

public class BlockingHandler implements RestHandler {

    private final Executor executor;

    public BlockingHandler(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void handle(QrsRequestContext requestContext) throws Exception {
        requestContext.suspend();
        requestContext.resume(executor);
    }
}
