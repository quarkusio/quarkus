package io.quarkus.runtime.util;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.jboss.logging.Logger;

/**
 * Forwards all method calls to the scheduled executor service returned from the {@link #delegate()} method.
 * Does not allow shutdown
 */
public class NoopShutdownScheduledExecutorService extends ForwardingScheduledExecutorService {

    private static final Logger LOG = Logger.getLogger(NoopShutdownScheduledExecutorService.class);

    private final ScheduledExecutorService delegate;

    public NoopShutdownScheduledExecutorService(final ScheduledExecutorService delegate) {
        this.delegate = delegate;
    }

    @Override
    protected ScheduledExecutorService delegate() {
        return delegate;
    }

    @Override
    public boolean isShutdown() {
        // managed executors are never shut down from the application's perspective
        return false;
    }

    @Override
    public boolean isTerminated() {
        // managed executors are never shut down from the application's perspective
        return false;
    }

    @Override
    public void shutdown() {
        LOG.debug("shutdown() not allowed on managed executor service");
    }

    @Override
    public List<Runnable> shutdownNow() {
        LOG.debug("shutdownNow() not allowed on managed executor service");
        return List.of();
    }

}
