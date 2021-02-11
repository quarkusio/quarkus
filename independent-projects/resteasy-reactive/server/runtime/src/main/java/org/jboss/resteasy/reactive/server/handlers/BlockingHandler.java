package org.jboss.resteasy.reactive.server.handlers;

import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.server.core.BlockingOperationSupport;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public class BlockingHandler implements ServerRestHandler {

    private volatile Executor executor;
    private final Supplier<Executor> supplier;

    public BlockingHandler(Supplier<Executor> supplier) {
        this.supplier = supplier;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        if (BlockingOperationSupport.isBlockingAllowed()) {
            return; //already dispatched
        }
        if (executor == null) {
            executor = supplier.get();
        }
        requestContext.suspend();
        requestContext.resume(executor);
    }
}
