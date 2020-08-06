package io.quarkus.qrs.runtime.handlers;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

import io.quarkus.qrs.runtime.core.QrsRequestContext;

public class BlockingHandler implements RestHandler {

    private volatile Executor executor;
    private final Supplier<Executor> supplier;

    public BlockingHandler(Supplier<Executor> supplier) {
        this.supplier = supplier;
    }

    @Override
    public void handle(QrsRequestContext requestContext) throws Exception {
        if (executor == null) {
            executor = supplier.get();
        }
        requestContext.suspend();
        requestContext.resume(executor);
    }
}
