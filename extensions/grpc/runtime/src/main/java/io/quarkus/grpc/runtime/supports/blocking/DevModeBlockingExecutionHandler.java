package io.quarkus.grpc.runtime.supports.blocking;

import java.util.concurrent.Callable;

class DevModeBlockingExecutionHandler implements Callable<Void> {

    final ClassLoader tccl;
    final Callable<Void> delegate;

    public DevModeBlockingExecutionHandler(ClassLoader tccl, Callable<Void> delegate) {
        this.tccl = tccl;
        this.delegate = delegate;
    }

    @Override
    public Void call() throws Exception {
        ClassLoader originalTccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(tccl);
        try {
            return delegate.call();
        } finally {
            Thread.currentThread().setContextClassLoader(originalTccl);
        }
    }
}
