package io.quarkus.runtime.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Forwards all method calls to the scheduled executor service returned from the {@link #delegate()} method. Only non-default
 * methods
 * declared on the {@link ScheduledExecutorService} interface are forwarded.
 */
public abstract class ForwardingScheduledExecutorService extends ForwardingExecutorService implements ScheduledExecutorService {

    protected abstract ScheduledExecutorService delegate();

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return delegate().schedule(command, delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return delegate().schedule(callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return delegate().scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return delegate().scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

}
