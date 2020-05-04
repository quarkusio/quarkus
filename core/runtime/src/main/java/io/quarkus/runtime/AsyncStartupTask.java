package io.quarkus.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The tasks that actually run recorded async startup
 */
public class AsyncStartupTask implements Runnable {

    private final AtomicInteger waitCount;
    private final StartupContext startupContext;
    private final StartupTask startupTask;
    private volatile AsyncStartupTask[] dependents;
    private final CompletableFuture<?> failureReporter;
    private final Executor executor;

    public AsyncStartupTask(int waitCount, StartupContext startupContext, StartupTask startupTask,
            CompletableFuture<?> failureReporter, Executor executor) {
        this.waitCount = new AtomicInteger(waitCount);
        this.startupContext = startupContext;
        this.startupTask = startupTask;
        this.failureReporter = failureReporter;
        this.executor = executor;
    }

    public AsyncStartupTask(int waitCount, CompletableFuture<?> cf, Executor executor) {
        this.waitCount = new AtomicInteger(waitCount);
        this.failureReporter = cf;
        this.executor = executor;
        this.startupContext = null;
        this.startupTask = new StartupTask() {
            @Override
            public void deploy(StartupContext context) {
                cf.complete(null);
            }
        };
        this.dependents = new AsyncStartupTask[0];
    }

    public void setDependents(AsyncStartupTask[] dependents) {
        this.dependents = dependents;
    }

    @Override
    public void run() {
        try {
            startupTask.deploy(startupContext);
            for (AsyncStartupTask i : dependents) {
                i.dependencyComplete();
            }
        } catch (Throwable t) {
            failureReporter.completeExceptionally(t);
        }
    }

    private void dependencyComplete() {
        if (waitCount.decrementAndGet() == 0) {
            executor.execute(this);
        }
    }

    public static ExecutorService newExecutor() {
        return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }
}
