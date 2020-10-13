package io.quarkus.rest.runtime.handlers;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.runtime.BlockingOperationControl;

public class BlockingHandler implements ServerRestHandler {

    private volatile Executor executor;
    private final Supplier<Executor> supplier;

    public BlockingHandler(Supplier<Executor> supplier) {
        this.supplier = supplier;
    }

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        if (BlockingOperationControl.isBlockingAllowed()) {
            return; //already dispatched
        }
        if (executor == null) {
            executor = supplier.get();
        }
        requestContext.suspend();
        requestContext.resume(executor);
    }
}
