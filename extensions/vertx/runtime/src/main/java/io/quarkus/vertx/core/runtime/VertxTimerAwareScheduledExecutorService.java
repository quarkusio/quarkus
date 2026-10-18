package io.quarkus.vertx.core.runtime;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.quarkus.runtime.util.ForwardingScheduledExecutorService;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

/**
 * A wrapper for {@link ScheduledExecutorService} that dispatches some of the operations using Vert.x timers
 * when called from Vert.x event-loops, else it just dispatches to a delegate {@link ScheduledExecutorService}.
 *
 * This class is used to keep some Mutiny scheduled operations on a Vert.x event-loop thread rather than hop
 * to a worker thread: delaying items, retries on failures, streams of periodic ticks, etc.
 */
public final class VertxTimerAwareScheduledExecutorService extends ForwardingScheduledExecutorService {

    private final ScheduledExecutorService delegate;

    public VertxTimerAwareScheduledExecutorService(ScheduledExecutorService delegate) {
        this.delegate = delegate;
    }

    @Override
    protected ScheduledExecutorService delegate() {
        return delegate;
    }

    // ------------------ ScheduledFuture for timers ------------------ //

    private static final class VertxTimerScheduledFuture<T> implements ScheduledFuture<T> {

        final Vertx vertx;
        final long timerId;
        volatile boolean cancelled;

        // Minimal wrapper class with just what is need to support cancellation
        VertxTimerScheduledFuture(Vertx vertx, long timerId) {
            this.vertx = vertx;
            this.timerId = timerId;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            throw new UnsupportedOperationException("getDelay is not implemented");
        }

        @Override
        public int compareTo(Delayed o) {
            throw new UnsupportedOperationException("compareTo is not implemented");
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            // No need for C&S, cancelTimer can be called multiple times
            cancelled = true;
            return vertx.cancelTimer(timerId);
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            throw new UnsupportedOperationException("isDone is not implemented");
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            throw new UnsupportedOperationException("get is not implemented");
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            throw new UnsupportedOperationException("get is not implemented");
        }
    }

    // ------------------ ScheduledExecutorService methods used by Mutiny ------------------ //

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        Context context = Vertx.currentContext();
        if (context != null) {
            Vertx vertx = context.owner();
            long timerId = vertx.setTimer(unit.toMillis(delay), new Handler<Long>() {
                @Override
                public void handle(Long tick) {
                    command.run();
                }
            });
            return new VertxTimerScheduledFuture<>(vertx, timerId);
        }
        return delegate.schedule(command, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        Context context = Vertx.currentContext();
        if (context != null) {
            Vertx vertx = context.owner();
            long timerId = vertx.setPeriodic(unit.toMillis(initialDelay), unit.toMillis(period), new Handler<Long>() {
                @Override
                public void handle(Long tick) {
                    command.run();
                }
            });
            return new VertxTimerScheduledFuture<Void>(vertx, timerId);
        }
        return delegate.scheduleAtFixedRate(command, initialDelay, period, unit);
    }
}
