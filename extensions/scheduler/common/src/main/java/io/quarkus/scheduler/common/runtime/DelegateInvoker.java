package io.quarkus.scheduler.common.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

import io.quarkus.scheduler.ScheduledExecution;

abstract class DelegateInvoker implements ScheduledInvoker {

    protected final ScheduledInvoker delegate;

    public DelegateInvoker(ScheduledInvoker delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isBlocking() {
        return delegate.isBlocking();
    }

    @Override
    public boolean isRunningOnVirtualThread() {
        return delegate.isRunningOnVirtualThread();
    }

    protected CompletionStage<Void> invokeDelegate(ScheduledExecution execution) {
        try {
            return delegate.invoke(execution);
        } catch (Throwable e) {
            return CompletableFuture.failedStage(e);
        }
    }

    protected void invokeComplete(CompletableFuture<Void> ret, ScheduledExecution execution) {
        invokeDelegate(execution).whenComplete(new BiConsumer<>() {
            @Override
            public void accept(Void r, Throwable t) {
                if (t != null) {
                    ret.completeExceptionally(t);
                } else {
                    ret.complete(null);
                }
            }
        });
    }
}
