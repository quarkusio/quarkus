package io.quarkus.core.impl;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import io.quarkus.core.AsyncStopContext;
import io.quarkus.core.AsyncVoidStartContext;
import io.quarkus.runtime.ShutdownContext;

/**
 * {@link AsyncVoidStartContext} implementation for sequential void service execution.
 * <p>
 * Stop handlers registered via {@link #onStop(Runnable)} or {@link #onStopAsync(Consumer)}
 * are delegated to the {@link ShutdownContext} provided at construction time.
 * <p>
 * For async void services, this implementation blocks the calling thread:
 * {@link #startComplete()} signals success, {@link #startFailed(Throwable)} wraps and rethrows,
 * and {@link #startCanceled()} signals cancellation.
 * <p>
 * Completion methods follow the precedence rules defined by {@link AsyncVoidStartContext}.
 */
public final class AsyncVoidStartContextImpl implements AsyncVoidStartContext {

    private static final int S_PENDING = 0;
    private static final int S_COMPLETED = 1;
    private static final int S_CANCELED = 2;
    private static final int S_FAILED = 3;

    private static final VarHandle stateHandle = ConstantBootstraps.fieldVarHandle(
            MethodHandles.lookup(), "state", VarHandle.class, AsyncVoidStartContextImpl.class, int.class);

    private final ShutdownContext shutdownContext;
    @SuppressWarnings("unused") // managed by VarHandle
    private volatile int state;
    private Throwable failure;

    /**
     * Construct a new instance.
     *
     * @param shutdownContext the shutdown context to delegate stop handlers to (must not be {@code null})
     */
    public AsyncVoidStartContextImpl(ShutdownContext shutdownContext) {
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
    public void startComplete() {
        // idempotent; throws only if canceled or failed
        int prev;
        do {
            prev = (int) stateHandle.getVolatile(this);
            if (prev == S_COMPLETED) {
                return;
            }
            if (prev != S_PENDING) {
                throw new IllegalStateException("Cannot complete: the service has already "
                        + (prev == S_CANCELED ? "been canceled" : "failed"));
            }
        } while ((int) stateHandle.compareAndExchange(this, prev, S_COMPLETED) != prev);
    }

    @Override
    public void startCanceled() {
        // idempotent; harmless no-op after completion; only failure is final
        int prev;
        do {
            prev = (int) stateHandle.getVolatile(this);
            if (prev == S_FAILED) {
                throw new IllegalStateException("Cannot cancel: the service has already failed");
            }
            if (prev == S_CANCELED || prev == S_COMPLETED) {
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
     * Check for failure after the action has completed.
     * This method should only be called after the action lambda has returned.
     *
     * @throws RuntimeException if the service start failed
     */
    public void checkFailure() {
        if (failure != null) {
            if (failure instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException("Service start failed", failure);
        }
    }

    /**
     * Determine whether the async action has signalled completion.
     *
     * @return {@code true} if {@link #startComplete()}, {@link #startCanceled()},
     *         or {@link #startFailed(Throwable)} has been called
     */
    public boolean isCompleted() {
        return (int) stateHandle.getVolatile(this) != S_PENDING;
    }
}
