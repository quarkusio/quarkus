package io.quarkus.vertx.core.runtime;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.jboss.logging.Logger;

import io.quarkus.runtime.util.ForwardingExecutorService;

/**
 * This executor is only used in the prod mode as the Vertx worker thread pool.
 */
class NoopShutdownExecutorService extends ForwardingExecutorService {

    private static final Logger LOG = Logger.getLogger(NoopShutdownExecutorService.class);

    private final ExecutorService delegate;

    NoopShutdownExecutorService(ExecutorService delegate) {
        this.delegate = delegate;
    }

    @Override
    protected ExecutorService delegate() {
        return delegate;
    }

    @Override
    public void shutdown() {
        LOG.debug("shutdown() deliberately not delegated");
    }

    @Override
    public List<Runnable> shutdownNow() {
        LOG.debug("shutdownNow() deliberately not delegated");
        return List.of();
    }

}
