package io.quarkus.grpc.runtime.supports.blocking;

import io.vertx.core.Handler;
import io.vertx.core.Promise;

class DevModeBlockingExecutionHandler implements Handler<Promise<Object>> {

    final ClassLoader tccl;
    final Handler<Promise<Object>> delegate;

    public DevModeBlockingExecutionHandler(ClassLoader tccl, Handler<Promise<Object>> delegate) {
        this.tccl = tccl;
        this.delegate = delegate;
    }

    @Override
    public void handle(Promise<Object> event) {
        ClassLoader originalTccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(tccl);
        try {
            delegate.handle(event);
        } finally {
            Thread.currentThread().setContextClassLoader(originalTccl);
        }
    }
}
