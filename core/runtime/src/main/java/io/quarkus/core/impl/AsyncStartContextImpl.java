package io.quarkus.core.impl;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import io.quarkus.core.AsyncStartContext;
import io.quarkus.core.AsyncStopContext;
import io.quarkus.runtime.ShutdownContext;

/**
 * {@link AsyncStartContext} implementation for sequential typed service execution.
 * <p>
 * Stop handlers registered via {@link #onStop(Runnable)} or {@link #onStopAsync(Consumer)}
 * are delegated to the {@link ShutdownContext} provided at construction time.
 * <p>
 * For async services, this implementation blocks the calling thread: {@link #startComplete(Object)}
 * stores the value, {@link #startFailed(Throwable)} wraps and rethrows, and
 * {@link #startCanceled()} stores {@code null}.
 * <p>
 * Passing {@code null} to {@link #startComplete(Object)} is an error; use a void service if
 * the service produces no value.
 * <p>
 * Completion methods follow the precedence rules defined by {@link AsyncStartContext}.
 *
 * @param <T> the service type
 */
public final class AsyncStartContextImpl<T> implements AsyncStartContext<T> {

    private static final int S_PENDING = 0;
    private static final int S_COMPLETED = 1;
    private static final int S_CANCELED = 2;
    private static final int S_FAILED = 3;

    private static final VarHandle stateHandle = ConstantBootstraps.fieldVarHandle(
            MethodHandles.lookup(), "state", VarHandle.class, AsyncStartContextImpl.class, int.class);

    private final ShutdownContext shutdownContext;
    @SuppressWarnings("unused") // managed by VarHandle
    private volatile int state;
    private T value;
    private Throwable failure;

    /**
     * Construct a new instance.
     *
     * @param shutdownContext the shutdown context to delegate stop handlers to (must not be {@code null})
     */
    public AsyncStartContextImpl(ShutdownContext shutdownContext) {
        this.shutdownContext = shutdownContext;
    }

    @Override
    public void onStopAsync(Consumer<AsyncStopContext> stopper) {
        // the latch blocks the shutdown thread until stopComplete() is called
        shutdownContext.addShutdownTask(() -> {
            CountDownLatch latch = new CountDownLatch(1);
            stopper.accept(latch::countDown);
            boolean intr = false;
            try {
                while (latch.getCount() > 0) {
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        intr = true;
                    }
                }
            } finally {
                if (intr) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    @Override
    public void startComplete(T value) {
        if (!stateHandle.compareAndSet(this, S_PENDING, S_COMPLETED)) {
            throw new IllegalStateException("A completion method has already been called");
        }
        if (value == null) {
            throw new IllegalArgumentException("Service value must not be null; use a void service instead");
        }
        this.value = value;
    }

    @Override
    public void startCanceled() {
        // cancellation is idempotent and harmless after completion; only failure is final
        int prev;
        do {
            prev = (int) stateHandle.getVolatile(this);
            if (prev == S_FAILED) {
                throw new IllegalStateException("Cannot cancel: the service has already failed");
            }
            if (prev == S_CANCELED) {
                return;
            }
        } while ((int) stateHandle.compareAndExchange(this, prev, S_CANCELED) != prev);
    }

    @Override
    public void startFailed(Throwable e) {
        if (!stateHandle.compareAndSet(this, S_PENDING, S_FAILED)) {
            throw new IllegalStateException("A completion method has already been called");
        }
        this.failure = e;
    }

    /**
     * Get the service value after the action has completed.
     * This method should only be called after the action lambda has returned.
     *
     * @return the service value, or {@code null} if the service was canceled
     * @throws RuntimeException if the service start failed
     */
    public T getValueOrThrow() {
        if (failure != null) {
            if (failure instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException("Service start failed", failure);
        }
        return value;
    }

    /**
     * Determine whether the async action has signalled completion.
     *
     * @return {@code true} if {@link #startComplete}, {@link #startCanceled},
     *         or {@link #startFailed} has been called
     */
    public boolean isCompleted() {
        return (int) stateHandle.getVolatile(this) != S_PENDING;
    }
}
