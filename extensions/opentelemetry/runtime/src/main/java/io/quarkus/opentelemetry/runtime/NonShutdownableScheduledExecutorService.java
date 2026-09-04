package io.quarkus.opentelemetry.runtime;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.quarkus.runtime.util.ForwardingScheduledExecutorService;

/**
 * A {@link ScheduledExecutorService} view over a shared, container-managed executor whose lifecycle is
 * <em>not</em> owned by the caller. Scheduling and execution are forwarded to the delegate, but the
 * lifecycle methods are neutralized: {@link #shutdown()}/{@link #shutdownNow()} are no-ops and
 * {@link #awaitTermination(long, TimeUnit)} returns immediately.
 * <p>
 * This is handed to OpenTelemetry's {@code PeriodicMetricReader}. That reader assumes it owns its executor
 * and, on shutdown, calls {@code shutdown()} followed by {@code awaitTermination(5s)}. Forwarding those to
 * the shared executor would (a) attempt to shut down an executor managed elsewhere and (b) block for the
 * full timeout waiting for a termination that never happens. Neutralizing them avoids both.
 * <p>
 * Note: this is a pragmatic stub. It makes the reader believe its tasks have terminated without actually
 * draining only the reader's own tasks; a more precise solution would give the reader a task-tracking
 * <em>view</em> of the shared pool (e.g. a scheduled variant of {@code org.jboss.threads.ViewExecutor}) so
 * that {@code shutdown()}/{@code awaitTermination()} drain exactly the reader's outstanding tasks.
 */
final class NonShutdownableScheduledExecutorService extends ForwardingScheduledExecutorService {

    private final ScheduledExecutorService delegate;
    private volatile boolean shutdown;

    NonShutdownableScheduledExecutorService(ScheduledExecutorService delegate) {
        this.delegate = delegate;
    }

    @Override
    protected ScheduledExecutorService delegate() {
        return delegate;
    }

    @Override
    public void shutdown() {
        shutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown = true;
        return List.of();
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public boolean isTerminated() {
        return shutdown;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        return true;
    }
}
