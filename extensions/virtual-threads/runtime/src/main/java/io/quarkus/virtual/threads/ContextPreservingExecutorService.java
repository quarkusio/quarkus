package io.quarkus.virtual.threads;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import io.vertx.core.Context;
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

    private static final class ContextPreservingRunnable implements Runnable {

        private final Runnable task;
        private final Context context;

        public ContextPreservingRunnable(Runnable task) {
            this.task = task;
            this.context = Vertx.currentContext();
        }

        @Override
        public void run() {
            if (context instanceof ContextInternal) {
                ContextInternal contextInternal = (ContextInternal) context;
                final var previousContext = contextInternal.beginDispatch();
                try {
                    task.run();
                } finally {
                    contextInternal.endDispatch(previousContext);
                }
            } else {
                task.run();
            }
        }
    }

    private static final class ContextPreservingCallable<T> implements Callable<T> {

        private final Callable<T> task;
        private final Context context;

        public ContextPreservingCallable(Callable<T> task) {
            this.task = task;
            this.context = Vertx.currentContext();
        }

        @Override
        public T call() throws Exception {
            if (context instanceof ContextInternal) {
                ContextInternal contextInternal = (ContextInternal) context;
                final var previousContext = contextInternal.beginDispatch();
                try {
                    return task.call();
                } finally {
                    contextInternal.endDispatch(previousContext);
                }
            } else {
                return task.call();
            }
        }
    }

    private static Runnable decorate(Runnable command) {
        Objects.requireNonNull(command);
        return new ContextPreservingRunnable(command);
    }

    private static <T> Callable<T> decorate(Callable<T> task) {
        Objects.requireNonNull(task);
        return new ContextPreservingCallable<>(task);
    }

    private static <T> Collection<? extends Callable<T>> decorateAll(Collection<? extends Callable<T>> tasks) {
        Objects.requireNonNull(tasks);
        return tasks.stream().map(ContextPreservingExecutorService::decorate).collect(Collectors.toList());
    }

    public void execute(final Runnable command) {
        delegate.execute(decorate(command));
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
        return delegate.submit(decorate(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return submit(Executors.callable(task, result));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(decorate(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(decorateAll(tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return delegate.invokeAll(decorateAll(tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(decorateAll(tasks));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(decorateAll(tasks), timeout, unit);
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
