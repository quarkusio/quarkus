package io.quarkus.scheduler.common.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
}
