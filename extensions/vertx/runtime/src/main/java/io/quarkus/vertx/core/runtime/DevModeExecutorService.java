package io.quarkus.vertx.core.runtime;

import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.runtime.util.ForwardingExecutorService;

/**
 * This executor is only used in the dev mode as the Vertx worker thread pool.
 * <p>
 * The underlying executor can be shut down and then replaced with a new re-initialized executor.
 */
class DevModeExecutorService extends ForwardingExecutorService {

    private static final Logger LOG = Logger.getLogger(DevModeExecutorService.class);

    private final Supplier<ExecutorService> initializer;
    private volatile ExecutorService executor;

    DevModeExecutorService(Supplier<ExecutorService> initializer) {
        this.initializer = initializer;
        this.executor = initializer.get();
    }

    @Override
    protected ExecutorService delegate() {
        return executor;
    }

    /**
     * Shutdown the underlying executor and then initialize a new one.
     */
    void reinit() {
        ExecutorService oldExecutor = this.executor;
        if (oldExecutor != null) {
            oldExecutor.shutdownNow();
        }
        this.executor = initializer.get();
        LOG.debug("Dev mode executor re-initialized");
    }

}
