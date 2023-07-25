package io.quarkus.virtual.threads;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;

/**
 * Delegating executor service implementation preserving the Vert.x context on {@link #execute(Runnable)}
 */
class ContextPreservingExecutorService implements ExecutorService {
    private final ExecutorService delegate;

    ContextPreservingExecutorService(final ExecutorService delegate) {
        this.delegate = delegate;
    }

    public void execute(final Runnable command) {
        var context = Vertx.currentContext();
        if (!(context instanceof ContextInternal)) {
            delegate.execute(command);
        } else {
            ContextInternal contextInternal = (ContextInternal) context;
            delegate.execute(new Runnable() {
                @Override
                public void run() {
                    final var previousContext = contextInternal.beginDispatch();
                    try {
                        command.run();
                    } finally {
                        contextInternal.endDispatch(previousContext);
                    }
                }
            });
        }
    }

    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return delegate.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(tasks, timeout, unit);
    }

    public void shutdown() {
        delegate.shutdown();
    }

    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    public String toString() {
        return delegate.toString();
    }
}
