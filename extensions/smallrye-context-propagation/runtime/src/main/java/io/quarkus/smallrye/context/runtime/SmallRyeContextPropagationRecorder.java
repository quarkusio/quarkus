package io.quarkus.smallrye.context.runtime;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ContextManagerExtension;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;

import io.smallrye.context.SmallRyeContextManager;
import io.smallrye.context.impl.DefaultValues;

/**
 * Builds the SmallRye context manager and the managed executors and thread contexts of the application.
 * <p>
 * The static methods of this class run from the services that the deployment module registers.
 */
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
    private static SmallRyeContextManager contextManager;

    /**
     * Register the context manager provider and prepare the context manager builder at static init.
     * <p>
     * A manager without context propagation and with an executor that rejects every task is registered
     * for the boot phase. {@link #configureRuntime(ExecutorService)} replaces it.
     *
     * @param providerClasses the discovered thread context provider classes
     * @param extensionClasses the discovered context manager extension classes
     */
    public static void configureStaticInit(List<Class<? extends ThreadContextProvider>> providerClasses,
            List<Class<? extends ContextManagerExtension>> extensionClasses) {
        List<ThreadContextProvider> discoveredProviders = new ArrayList<>();
        for (Class<? extends ThreadContextProvider> provider : providerClasses) {
            discoveredProviders.add(instantiate(provider));
        }
        List<ContextManagerExtension> discoveredExtensions = new ArrayList<>();
        for (Class<? extends ContextManagerExtension> extension : extensionClasses) {
            discoveredExtensions.add(instantiate(extension));
        }
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

    private static <T> T instantiate(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException("Failed to instantiate declared ThreadContextProvider class: " + clazz.getName(), e);
        }
    }

    /**
     * Build the context manager with the application executor and register it for the runtime class loader.
     *
     * @param executorService the application executor
     */
    public static void configureRuntime(ExecutorService executorService) {
        // associate the static init manager to the runtime CL
        ContextManagerProvider contextManagerProvider = ContextManagerProvider.instance();
        // finish building our manager
        builder.withDefaultExecutorService(executorService);

        contextManager = builder.build();

        contextManagerProvider.registerContextManager(contextManager, Thread.currentThread().getContextClassLoader());
        //Avoid leaking the classloader:
        SmallRyeContextPropagationRecorder.builder = null;
    }

    /**
     * Release the context manager built by {@link #configureRuntime(ExecutorService)}, if any.
     */
    public static void releaseContextManager() {
        SmallRyeContextManager manager = contextManager;
        if (manager != null) {
            contextManager = null;
            ContextManagerProvider.instance().releaseContextManager(manager);
        }
    }

    /**
     * Create a managed executor from the settings of a {@code @ManagedExecutorConfig} injection point.
     *
     * @param definition the captured settings
     * @return the managed executor
     */
    public static ManagedExecutor createManagedExecutor(ManagedExecutorDefinition definition) {
        return ManagedExecutor.builder()
                .propagated(definition.propagated().toArray(String[]::new))
                .cleared(definition.cleared().toArray(String[]::new))
                .maxAsync(definition.maxAsync())
                .maxQueued(definition.maxQueued())
                .build();
    }

    /**
     * Create a thread context from the settings of a {@code @ThreadContextConfig} injection point.
     *
     * @param definition the captured settings
     * @return the thread context
     */
    public static ThreadContext createThreadContext(ThreadContextDefinition definition) {
        return ThreadContext.builder()
                .propagated(definition.propagated().toArray(String[]::new))
                .cleared(definition.cleared().toArray(String[]::new))
                .unchanged(definition.unchanged().toArray(String[]::new))
                .build();
    }
}
