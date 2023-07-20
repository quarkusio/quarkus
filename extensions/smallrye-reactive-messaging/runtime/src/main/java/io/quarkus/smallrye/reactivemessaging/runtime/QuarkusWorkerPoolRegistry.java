package io.quarkus.smallrye.reactivemessaging.runtime;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.ExecutorRecorder;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.providers.connectors.ExecutionHolder;
import io.smallrye.reactive.messaging.providers.connectors.WorkerPoolRegistry;
import io.smallrye.reactive.messaging.providers.helpers.Validation;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.impl.ContextInternal;
import io.vertx.mutiny.core.Context;
import io.vertx.mutiny.core.WorkerExecutor;

@Alternative
@Priority(1)
@ApplicationScoped
// TODO: create a different entry for WorkerPoolRegistry than `analyzeWorker` and drop this class
public class QuarkusWorkerPoolRegistry extends WorkerPoolRegistry {

    private static final Logger logger = Logger.getLogger(QuarkusWorkerPoolRegistry.class);
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

    private enum VirtualExecutorSupplier implements Supplier<Executor> {
        Instance;

        private final Executor executor;

        /**
         * This method uses reflection in order to allow developers to quickly test quarkus-loom without needing to
         * change --release, --source, --target flags and to enable previews.
         * Since we try to load the "Loom-preview" classes/methods at runtime, the application can even be compiled
         * using java 11 and executed with a loom-compliant JDK.
         */
        VirtualExecutorSupplier() {
            Executor actual;
            try {
                var virtual = (Executor) Executors.class.getMethod("newVirtualThreadPerTaskExecutor")
                        .invoke(this);
                actual = new Executor() {
                    @Override
                    public void execute(Runnable command) {
                        var context = Vertx.currentContext();
                        if (!(context instanceof ContextInternal)) {
                            virtual.execute(command);
                        } else {
                            ContextInternal contextInternal = (ContextInternal) context;
                            virtual.execute(new Runnable() {
                                @Override
                                public void run() {
                                    final var previousContext = contextInternal.beginDispatch();
                                    try {
                                        command.run();
                                    } finally {
                                        contextInternal.endDispatch(previousContext);
                                    }
                                }
                            });
                        }
                    }
                };
            } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                //quite ugly but works
                logger.warnf(e, "You weren't able to create an executor that spawns virtual threads, the default" +
                        " blocking executor will be used, please check that your JDK is compatible with " +
                        "virtual threads");
                //if for some reason a class/method can't be loaded or invoked we return the traditional EXECUTOR
                actual = ExecutorRecorder.getCurrent();
            }
            this.executor = actual;
        }

        @Override
        public Executor get() {
            return this.executor;
        }
    }

    public void terminate(
            @Observes(notifyObserver = Reception.IF_EXISTS) @Priority(100) @BeforeDestroyed(ApplicationScoped.class) Object event) {
        if (!workerExecutors.isEmpty()) {
            for (WorkerExecutor executor : workerExecutors.values()) {
                executor.close();
            }
        }
    }

    public <T> Uni<T> executeWork(Context currentContext, Uni<T> uni, String workerName, boolean ordered) {
        Objects.requireNonNull(uni, "Action to execute not provided");

        if (workerName == null) {
            if (currentContext != null) {
                return currentContext.executeBlocking(Uni.createFrom().deferred(() -> uni), ordered);
            }
            return executionHolder.vertx().executeBlocking(uni, ordered);
        } else if (virtualThreadWorkers.contains(workerName)) {
            return runOnVirtualThread(currentContext, uni);
        } else {
            if (currentContext != null) {
                return getWorker(workerName).executeBlocking(uni, ordered)
                        .onItemOrFailure().transformToUni((item, failure) -> {
                            return Uni.createFrom().emitter(emitter -> {
                                if (failure != null) {
                                    currentContext.runOnContext(() -> emitter.fail(failure));
                                } else {
                                    currentContext.runOnContext(() -> emitter.complete(item));
                                }
                            });
                        });
            }
            return getWorker(workerName).executeBlocking(uni, ordered);
        }
    }

    private <T> Uni<T> runOnVirtualThread(Context currentContext, Uni<T> uni) {
        return uni.runSubscriptionOn(VirtualExecutorSupplier.Instance.get())
                .onItemOrFailure().transformToUni((item, failure) -> {
                    return Uni.createFrom().emitter(emitter -> {
                        if (currentContext != null) {
                            if (failure != null) {
                                currentContext.runOnContext(() -> emitter.fail(failure));
                            } else {
                                currentContext.runOnContext(() -> emitter.complete(item));
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
                        LoggerFactory.getLogger(WorkerPoolRegistry.class)
                                .info("Created worker pool named " + workerName + " with concurrency of "
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
