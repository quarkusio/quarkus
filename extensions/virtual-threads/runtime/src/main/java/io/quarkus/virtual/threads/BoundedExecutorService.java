package io.quarkus.virtual.threads;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import io.quarkus.runtime.util.ForwardingExecutorService;

/**
 * An {@link ExecutorService} wrapper that limits the number of concurrently executing tasks
 * using a {@link Semaphore}. Tasks are still submitted to the delegate immediately (so the
 * caller is never blocked), but the task payload acquires a permit before running and releases
 * it on completion.
 * <p>
 * All work-dispatching methods ({@code execute}, {@code submit}, {@code invokeAll},
 * {@code invokeAny}) are overridden to ensure the semaphore is respected.
 * The semaphore uses fair ordering to prevent starvation under sustained load.
 */
class BoundedExecutorService extends ForwardingExecutorService {

    private final ExecutorService delegate;
    private final Semaphore semaphore;

    BoundedExecutorService(ExecutorService delegate, int maxConcurrency) {
        this.delegate = delegate;
        this.semaphore = new Semaphore(maxConcurrency, true);
    }

    @Override
    protected ExecutorService delegate() {
        return delegate;
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(wrapRunnable(command));
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(wrapCallable(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(wrapRunnable(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(wrapRunnable(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(wrapCallables(tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return delegate.invokeAll(wrapCallables(tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(wrapCallables(tasks));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(wrapCallables(tasks), timeout, unit);
    }

    int availablePermits() {
        return semaphore.availablePermits();
    }

    int queueLength() {
        return semaphore.getQueueLength();
    }

    private Runnable wrapRunnable(Runnable command) {
        return () -> {
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            try {
                command.run();
            } finally {
                semaphore.release();
            }
        };
    }

    private <T> Callable<T> wrapCallable(Callable<T> task) {
        return () -> {
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
            try {
                return task.call();
            } finally {
                semaphore.release();
            }
        };
    }

    private <T> List<Callable<T>> wrapCallables(Collection<? extends Callable<T>> tasks) {
        return tasks.stream().map(this::wrapCallable).collect(Collectors.toList());
    }
}
