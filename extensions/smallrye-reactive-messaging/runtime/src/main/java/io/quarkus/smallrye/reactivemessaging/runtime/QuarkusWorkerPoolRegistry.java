package io.quarkus.smallrye.reactivemessaging.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;

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
import io.vertx.mutiny.core.Context;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.WorkerExecutor;

@Alternative
@Priority(1)
@ApplicationScoped
// TODO: create a different entry for WorkerPoolRegistry than `analyzeWorker` and drop this class
public class QuarkusWorkerPoolRegistry extends WorkerPoolRegistry {

    private static final Logger log = Logger.getLogger(WorkerPoolRegistry.class);

    private static final String WORKER_CONFIG_PREFIX = "smallrye.messaging.worker";
    private static final String WORKER_CONCURRENCY = "max-concurrency";
    public static final String DEFAULT_VIRTUAL_THREAD_WORKER = "<virtual-thread>";

    @Inject
    ExecutionHolder executionHolder;

    private final Map<String, Integer> workerConcurrency = new HashMap<>();
    private final Map<String, WorkerExecutor> workerExecutors = new ConcurrentHashMap<>();
    private final Set<String> virtualThreadWorkers = initVirtualThreadWorkers();

    private static Set<String> initVirtualThreadWorkers() {
        Set<String> set = new ConcurrentHashSet<>();
        set.add(DEFAULT_VIRTUAL_THREAD_WORKER);
        return set;
    }

    public void terminate(
            @Observes(notifyObserver = Reception.IF_EXISTS) @Priority(100) @BeforeDestroyed(ApplicationScoped.class) Object event) {
        if (!workerExecutors.isEmpty()) {
            for (WorkerExecutor executor : workerExecutors.values()) {
                if (Infrastructure.canCallerThreadBeBlocked()) {
                    executor.closeAndAwait();
                } else {
                    executor.closeAndForget();
                }
            }
        }
    }

    public <T> Uni<T> executeWork(Context msgContext, Uni<T> uni, String workerName, boolean ordered) {
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
        if (workerConcurrency.containsKey(workerName)) {
            WorkerExecutor executor = workerExecutors.get(workerName);
            if (executor == null) {
                synchronized (this) {
                    executor = workerExecutors.get(workerName);
                    if (executor == null) {
                        executor = executionHolder.vertx().createSharedWorkerExecutor(workerName,
                                workerConcurrency.get(workerName));
                        log.info("Created worker pool named " + workerName + " with concurrency of "
                                + workerConcurrency.get(workerName));
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

            // Validate @Blocking worker pool has configuration to define concurrency
            String workerConfigKey = WORKER_CONFIG_PREFIX + "." + poolName + "." + WORKER_CONCURRENCY;
            Optional<Integer> concurrency = ConfigProvider.getConfig().getOptionalValue(workerConfigKey, Integer.class);
            if (concurrency.isEmpty()) {
                throw getBlockingError(className, method, workerConfigKey + " was not defined");
            }

            workerConcurrency.put(poolName, concurrency.get());
        }
    }

    private IllegalArgumentException getBlockingError(String className, String method, String message) {
        return new IllegalArgumentException(
                "Invalid method annotated with @Blocking: " + className + "#" + method + " - " + message);
    }

}
