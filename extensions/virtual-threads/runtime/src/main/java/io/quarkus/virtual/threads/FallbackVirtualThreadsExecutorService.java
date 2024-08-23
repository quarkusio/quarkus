package io.quarkus.virtual.threads;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;

/**
 * Fallback executor service implementation in case the virtual threads are disabled or not available on the current platform.
 * <p>
 * Executes tasks on the current Vert.x context worker pool, or when not available, on the Mutiny Infrastructure default worker
 * pool
 * Shutdown methods are no-op as the executor service is a wrapper around these previous execute methods.
 */
class FallbackVirtualThreadsExecutorService extends AbstractExecutorService {

    private AtomicBoolean shutdown = new AtomicBoolean();

    @Override
    public void execute(Runnable command) {
        var context = Vertx.currentContext();
        if (!(context instanceof ContextInternal)) {
            Infrastructure.getDefaultWorkerPool().execute(command);
        } else {
            context.executeBlocking(() -> {
                command.run();
                return null;
            }, false);
        }
    }

    @Override
    public void shutdown() {
        shutdown.compareAndSet(false, true);
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown.compareAndSet(false, true);
        return Collections.EMPTY_LIST;
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    public boolean isTerminated() {
        return shutdown.get();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        shutdown.compareAndSet(false, true);
        return true;
    }
}
