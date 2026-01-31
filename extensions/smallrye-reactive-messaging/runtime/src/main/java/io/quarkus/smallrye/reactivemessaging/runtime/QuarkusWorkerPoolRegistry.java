package io.quarkus.smallrye.reactivemessaging.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.virtual.threads.VirtualThreadsRecorder;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.providers.connectors.ExecutionHolder;
import io.smallrye.reactive.messaging.providers.connectors.WorkerPoolRegistry;
import io.smallrye.reactive.messaging.providers.helpers.Validation;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.WorkerExecutorInternal;
import io.vertx.mutiny.core.Context;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.WorkerExecutor;

@Alternative
@Priority(1)
@ApplicationScoped
// TODO: create a different entry for WorkerPoolRegistry than `analyzeWorker` and drop this class
public class QuarkusWorkerPoolRegistry extends WorkerPoolRegistry {

    private static final Logger log = Logger.getLogger(WorkerPoolRegistry.class);

    public static final String DEFAULT_VIRTUAL_THREAD_WORKER = "<virtual-thread>";

    @Inject
    ExecutionHolder executionHolder;

    private final Map<String, WorkerPoolConfig> workerConfig = new HashMap<>();
    private final Map<String, WorkerExecutor> workerExecutors = new ConcurrentHashMap<>();
    private final Set<String> virtualThreadWorkers = initVirtualThreadWorkers();
    private volatile boolean closed = false;

    private static Set<String> initVirtualThreadWorkers() {
        Set<String> set = new ConcurrentHashSet<>();
        set.add(DEFAULT_VIRTUAL_THREAD_WORKER);
        return set;
    }

    public void terminate(
            @Observes(notifyObserver = Reception.IF_EXISTS) @Priority(Interceptor.Priority.PLATFORM_BEFORE) @BeforeDestroyed(ApplicationScoped.class) Object event) {
        log.debug("Terminating messaging worker pools");
        if (!workerExecutors.isEmpty()) {
            closed = true;
            // In most cases this is called from the main thread, so we can block.
            if (!Infrastructure.canCallerThreadBeBlocked()) {
                for (WorkerExecutor executor : workerExecutors.values()) {
                    executor.closeAndForget();
                }
                return;
            }

            // Shutdown all worker executors
            for (Map.Entry<String, WorkerExecutor> entry : workerExecutors.entrySet()) {
                WorkerExecutor executor = entry.getValue();
                // Get the underlying Vert.x WorkerPool and shutdown its executor
                try {
                    ((WorkerExecutorInternal) executor.getDelegate()).getPool().executor().shutdown();
                } catch (Exception e) {
                    log.warnf(e, "Failed to shutdown worker pool %s", entry.getKey());
                }
            }

            long start = System.nanoTime();
            boolean terminated = false;
            int loop = 1;
            long elapsed = 0;

            while (!terminated) {
                terminated = true;
                for (Map.Entry<String, WorkerExecutor> entry : workerExecutors.entrySet()) {
                    WorkerExecutor workerExecutor = entry.getValue();
                    ExecutorService innerExecutor = ((WorkerExecutorInternal) workerExecutor.getDelegate()).getPool()
                            .executor();
                    WorkerPoolConfig poolConfig = workerConfig.get(entry.getKey());
                    long timeout = poolConfig != null ? poolConfig.shutdownTimeout().toNanos()
                            : TimeUnit.MILLISECONDS.toNanos(DEFAULT_SHUTDOWN_TIMEOUT_MS);
                    long interval = poolConfig != null ? poolConfig.shutdownCheckInterval().toNanos()
                            : TimeUnit.MILLISECONDS.toNanos(DEFAULT_SHUTDOWN_CHECK_INTERVAL_MS);
                    log.debugf("Await termination loop: %d, worker: %s, remaining: %d ns",
                            Integer.valueOf(loop), entry.getKey(), Long.valueOf(timeout - elapsed));
                    try {
                        if (!innerExecutor.awaitTermination(Math.min(timeout - elapsed, interval), TimeUnit.NANOSECONDS)) {
                            elapsed = System.nanoTime() - start;
                            if (elapsed >= timeout) {
                                log.warnf("Worker pool %s did not terminate within timeout, forcing shutdown", entry.getKey());
                                innerExecutor.shutdownNow();
                                workerExecutor.closeAndAwait();
                            } else {
                                terminated = false; // Still waiting
                            }
                        }
                    } catch (InterruptedException e) {
                        log.warnf(e,
                                "Interrupted while waiting for worker pools to terminate, forcing shutdown of all remaining pools");
                        // Force shutdown all remaining worker pools
                        for (Map.Entry<String, WorkerExecutor> remaining : workerExecutors.entrySet()) {
                            try {
                                WorkerExecutor exec = remaining.getValue();
                                ExecutorService execService = ((WorkerExecutorInternal) exec.getDelegate()).getPool()
                                        .executor();
                                execService.shutdownNow();
                                exec.closeAndAwait();
                            } catch (Exception ex) {
                                log.warnf(ex, "Error forcing shutdown of worker pool %s after interruption",
                                        remaining.getKey());
                            }
                        }
                        terminated = true; // Exit the loop
                    } catch (Exception e) {
                        log.warnf(e, "Error during shutdown of worker pool %s", entry.getKey());
                    }
                }
                loop++;
            }
        }
    }

    @Override
    public <T> Uni<T> executeWork(Context msgContext, Uni<T> uni, String workerName, boolean ordered) {
        if (closed) {
            return Uni.createFrom().failure(new RejectedExecutionException("WorkerPoolRegistry is being shut down"));
        }
        Objects.requireNonNull(uni, "Action to execute not provided");
        if (workerName == null) {
            if (msgContext != null) {
                return msgContext.executeBlocking(uni, ordered);
            }
            return executionHolder.vertx().executeBlocking(uni, ordered);
        } else if (virtualThreadWorkers.contains(workerName)) {
            return runOnVirtualThread(msgContext, uni);
        } else {
            return runOnWorkerThread(msgContext, uni, workerName, ordered);
        }
    }

    private <T> Uni<T> runOnWorkerThread(Context msgContext, Uni<T> uni, String workerName, boolean ordered) {
        WorkerExecutor worker = getWorker(workerName);
        if (msgContext != null) {
            return worker.executeBlocking(uniOnMessageContext(uni, msgContext), ordered)
                    .onItemOrFailure().transformToUni((item, failure) -> {
                        return Uni.createFrom().emitter(emitter -> {
                            if (failure != null) {
                                msgContext.runOnContext(() -> emitter.fail(failure));
                            } else {
                                msgContext.runOnContext(() -> emitter.complete(item));
                            }
                        });
                    });
        }
        return worker.executeBlocking(uni, ordered);
    }

    private static <T> Uni<T> uniOnMessageContext(Uni<T> uni, Context msgContext) {
        return msgContext != Vertx.currentContext() ? uni
                .runSubscriptionOn(r -> new ContextPreservingRunnable(r, msgContext).run())
                : uni;
    }

    private <T> Uni<T> runOnVirtualThread(Context msgContext, Uni<T> uni) {
        ExecutorService vtExecutor = VirtualThreadsRecorder.getCurrent();
        return uniOnMessageContext(uni, msgContext, vtExecutor)
                .onItemOrFailure().transformToUni((item, failure) -> {
                    return Uni.createFrom().emitter(emitter -> {
                        if (msgContext != null) {
                            if (failure != null) {
                                msgContext.runOnContext(() -> emitter.fail(failure));
                            } else {
                                msgContext.runOnContext(() -> emitter.complete(item));
                            }
                        } else {
                            // Some method do not have a context (generator methods)
                            if (failure != null) {
                                emitter.fail(failure);
                            } else {
                                emitter.complete(item);
                            }
                        }
                    });
                });
    }

    private static <T> Uni<T> uniOnMessageContext(Uni<T> uni, Context msgContext, ExecutorService vtExecutor) {
        return msgContext != Vertx.currentContext()
                ? uni.runSubscriptionOn(r -> vtExecutor.execute(new ContextPreservingRunnable(r, msgContext)))
                : uni.runSubscriptionOn(vtExecutor);
    }

    private static final class ContextPreservingRunnable implements Runnable {

        private final Runnable task;
        private final io.vertx.core.Context context;

        public ContextPreservingRunnable(Runnable task, Context context) {
            this.task = task;
            this.context = context.getDelegate();
        }

        @Override
        public void run() {
            if (context instanceof ContextInternal) {
                ContextInternal contextInternal = (ContextInternal) context;
                final var previousContext = contextInternal.beginDispatch();
                try {
                    task.run();
                } finally {
                    contextInternal.endDispatch(previousContext);
                }
            } else {
                task.run();
            }
        }
    }

    public WorkerExecutor getWorker(String workerName) {
        Objects.requireNonNull(workerName, "Worker Name not specified");

        if (workerExecutors.containsKey(workerName)) {
            return workerExecutors.get(workerName);
        }
        if (workerConfig.containsKey(workerName)) {
            WorkerExecutor executor = workerExecutors.get(workerName);
            if (executor == null) {
                synchronized (this) {
                    executor = workerExecutors.get(workerName);
                    if (executor == null) {
                        WorkerPoolConfig config = workerConfig.get(workerName);
                        executor = executionHolder.vertx().createSharedWorkerExecutor(workerName,
                                config.maxConcurrency());
                        log.infof("Created worker pool named %s with concurrency of %d", workerName,
                                config.maxConcurrency());
                        workerExecutors.put(workerName, executor);
                    }
                }
            }
            if (executor != null) {
                return executor;
            } else {
                throw new RuntimeException("Failed to create Worker for " + workerName);
            }
        }

        // Shouldn't get here
        throw new IllegalArgumentException("@Blocking referred to invalid worker name. " + workerName);
    }

    public void defineWorker(String className, String method, String poolName, boolean virtualThread) {
        Objects.requireNonNull(className, "className was empty");
        Objects.requireNonNull(method, "Method was empty");
        if (virtualThread) {
            virtualThreadWorkers.add(poolName);
            return;
        }

        if (!poolName.equals(Blocking.DEFAULT_WORKER_POOL)) {
            // Validate @Blocking value is not empty, if set
            if (Validation.isBlank(poolName)) {
                throw getBlockingError(className, method, "value is blank or null");
            }

            Config config = ConfigProvider.getConfig();
            // Validate @Blocking worker pool has configuration to define concurrency
            String maxConcurrencyConfigKey = WORKER_CONFIG_PREFIX + "." + poolName + "." + WORKER_CONCURRENCY;
            String shutdownTimeoutConfigKey = WORKER_CONFIG_PREFIX + "." + poolName + "." + SHUTDOWN_TIMEOUT;
            String shutdownCheckIntervalConfigKey = WORKER_CONFIG_PREFIX + "." + poolName + "." + SHUTDOWN_CHECK_INTERVAL;

            Optional<Integer> concurrency = config.getOptionalValue(maxConcurrencyConfigKey, Integer.class);
            if (concurrency.isEmpty()) {
                throw getBlockingError(className, method, maxConcurrencyConfigKey + " was not defined");
            }
            int maxConcurrency = concurrency.get();
            // Get optional shutdown timeout and check interval configurations
            int shutdownTimeout = config
                    .getOptionalValue(shutdownTimeoutConfigKey, Integer.class)
                    .orElse(DEFAULT_SHUTDOWN_TIMEOUT_MS);
            int shutdownCheckInterval = config
                    .getOptionalValue(shutdownCheckIntervalConfigKey, Integer.class)
                    .orElse(DEFAULT_SHUTDOWN_CHECK_INTERVAL_MS);

            workerConfig.put(poolName,
                    new WorkerPoolConfig(maxConcurrency, shutdownTimeout, shutdownCheckInterval));
        }
    }

    private IllegalArgumentException getBlockingError(String className, String method, String message) {
        return new IllegalArgumentException(
                "Invalid method annotated with @Blocking: " + className + "#" + method + " - " + message);
    }

}
