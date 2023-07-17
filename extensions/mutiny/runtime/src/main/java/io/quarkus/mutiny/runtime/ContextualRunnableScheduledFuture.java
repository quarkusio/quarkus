package io.quarkus.mutiny.runtime;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.threads.ContextHandler;

class ContextualRunnableScheduledFuture<V> implements RunnableScheduledFuture<V> {
    private final RunnableScheduledFuture<V> runnable;
    private final Object context;
    private final ContextHandler<Object> contextHandler;

    public ContextualRunnableScheduledFuture(ContextHandler<Object> contextHandler, Object context,
            RunnableScheduledFuture<V> runnable) {
        this.contextHandler = contextHandler;
        this.context = context;
        this.runnable = runnable;
    }

    @Override
    public boolean isPeriodic() {
        return runnable.isPeriodic();
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return runnable.getDelay(unit);
    }

    @Override
    public int compareTo(Delayed o) {
        return runnable.compareTo(o);
    }

    @Override
    public void run() {
        if (contextHandler != null) {
            contextHandler.runWith(runnable, context);
        } else {
            runnable.run();
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return runnable.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return runnable.isCancelled();
    }

    @Override
    public boolean isDone() {
        return runnable.isDone();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return runnable.get();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return runnable.get(timeout, unit);
    }
}
