package io.quarkus.virtual.threads;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import io.quarkus.runtime.util.ForwardingExecutorService;

/**
 * An implementation of {@code ExecutorService} that delegates to the real executor, while disallowing termination.
 */
class DelegatingExecutorService extends ForwardingExecutorService {
    private final ExecutorService delegate;

    DelegatingExecutorService(final ExecutorService delegate) {
        this.delegate = delegate;
    }

    @Override
    protected ExecutorService delegate() {
        return delegate;
    }

    public boolean isShutdown() {
        // container managed executors are never shut down from the application's perspective
        return false;
    }

    public boolean isTerminated() {
        // container managed executors are never shut down from the application's perspective
        return false;
    }

    public boolean awaitTermination(final long timeout, final TimeUnit unit) {
        return false;
    }

    public void shutdown() {
        throw new UnsupportedOperationException("shutdown not allowed on managed executor service");
    }

    public List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException("shutdownNow not allowed on managed executor service");
    }

    public String toString() {
        return delegate.toString();
    }
}
