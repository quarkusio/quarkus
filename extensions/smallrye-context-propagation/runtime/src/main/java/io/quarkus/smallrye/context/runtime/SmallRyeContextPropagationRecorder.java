package io.quarkus.smallrye.context.runtime;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ContextManagerExtension;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.context.SmallRyeContextManager;
import io.smallrye.context.SmallRyeManagedExecutor;
import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.context.impl.DefaultValues;

/**
 * The runtime value service used to create values related to the MP-JWT services
 */
@Recorder
public class SmallRyeContextPropagationRecorder {

    private static final ExecutorService NOPE_EXECUTOR_SERVICE = new ExecutorService() {

        @Override
        public void execute(Runnable command) {
            nope();
        }

        @Override
        public void shutdown() {
            nope();
        }

        @Override
        public List<Runnable> shutdownNow() {
            nope();
            return null;
        }

        @Override
        public boolean isShutdown() {
            nope();
            return false;
        }

        @Override
        public boolean isTerminated() {
            nope();
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            nope();
            return false;
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            nope();
            return null;
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            nope();
            return null;
        }

        @Override
        public Future<?> submit(Runnable task) {
            nope();
            return null;
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            nope();
            return null;
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException {
            nope();
            return null;
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
                throws InterruptedException, ExecutionException {
            nope();
            return null;
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            nope();
            return null;
        }

        private void nope() {
            throw new RuntimeException(
                    "Trying to invoke ContextPropagation on a partially-configured ContextManager instance. You should wait until runtime init is done. You can do that by consuming the ContextPropagationBuildItem.");
        }
    };
    private static SmallRyeContextManager.Builder builder;

    public void configureStaticInit(List<ThreadContextProvider> discoveredProviders,
            List<ContextManagerExtension> discoveredExtensions) {
        // build the manager at static init time
        // in the live-reload mode, the provider instance may be already set in the previous start
        if (ContextManagerProvider.INSTANCE.get() == null) {
            ContextManagerProvider contextManagerProvider = new QuarkusContextManagerProvider();
            ContextManagerProvider.register(contextManagerProvider);
        }

        // do what config we can here, but we need the runtime executor service to finish
        builder = (SmallRyeContextManager.Builder) ContextManagerProvider.instance()
                .getContextManagerBuilder();
        builder.withThreadContextProviders(discoveredProviders.toArray(new ThreadContextProvider[0]));
        builder.withContextManagerExtensions(discoveredExtensions.toArray(new ContextManagerExtension[0]));

        // During boot, if anyone is using CP, they will get no propagation and an error if they try to use
        // the executor. This is (so far) only for spring-cloud-config-client which uses Vert.x via Mutiny
        // to load config before we're ready for runtime init
        SmallRyeContextManager.Builder noContextBuilder = (SmallRyeContextManager.Builder) ContextManagerProvider.instance()
                .getContextManagerBuilder();
        noContextBuilder.withThreadContextProviders(new ThreadContextProvider[0]);
        noContextBuilder.withContextManagerExtensions(new ContextManagerExtension[0]);
        noContextBuilder.withDefaultExecutorService(NOPE_EXECUTOR_SERVICE);
        noContextBuilder.withDefaultValues(DefaultValues.empty());
        ContextManagerProvider.instance().registerContextManager(noContextBuilder.build(), null /* not used */);
    }

    public void configureRuntime(ExecutorService executorService, ShutdownContext shutdownContext) {
        // associate the static init manager to the runtime CL
        ContextManagerProvider contextManagerProvider = ContextManagerProvider.instance();
        // finish building our manager
        builder.withDefaultExecutorService(executorService);

        SmallRyeContextManager contextManager = builder.build();

        contextManagerProvider.registerContextManager(contextManager, Thread.currentThread().getContextClassLoader());
        //needs to be late, as running threads can re-create an implicit one
        shutdownContext.addLastShutdownTask(new Runnable() {
            @Override
            public void run() {
                contextManagerProvider.releaseContextManager(contextManager);
            }
        });
        //Avoid leaking the classloader:
        SmallRyeContextPropagationRecorder.builder = null;
    }

    public Supplier<Object> initializeManagedExecutor(ExecutorService executorService) {
        return new Supplier<Object>() {
            @Override
            public Object get() {
                ThreadContext threadContext = Arc.container().instance(ThreadContext.class).get();
                return new SmallRyeManagedExecutor(-1, -1, (SmallRyeThreadContext) threadContext, executorService,
                        "no-ip") {
                    @Override
                    public void shutdown() {
                        throw new IllegalStateException("This executor is managed by the container and cannot be shut down.");
                    }

                    @Override
                    public List<Runnable> shutdownNow() {
                        throw new IllegalStateException("This executor is managed by the container and cannot be shut down.");
                    }
                };
            }
        };
    }

    public Supplier<Object> initializeConfiguredThreadContext(String[] cleared, String[] propagated, String[] unchanged) {
        return new Supplier<Object>() {
            @Override
            public Object get() {
                return ThreadContext.builder().propagated(propagated).cleared(cleared).unchanged(unchanged).build();
            }
        };
    }

    public Supplier<Object> initializeConfiguredManagedExecutor(String[] cleared, String[] propagated, int maxAsync,
            int maxQueued) {
        return new Supplier<Object>() {
            @Override
            public Object get() {
                return ManagedExecutor.builder().propagated(propagated).cleared(cleared).maxAsync(maxAsync).maxQueued(maxQueued)
                        .build();
            }
        };
    }
}
