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

import org.eclipse.microprofile.context.spi.ContextManagerExtension;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;

import io.smallrye.context.SmallRyeContextManager;
import io.smallrye.context.impl.DefaultValues;

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

    public static void configureStaticInit(List<Class<?>> providerClasses, List<Class<?>> extensionClasses) {
        List<ThreadContextProvider> discoveredProviders = new ArrayList<>();
        for (Class<?> provider : providerClasses) {
            discoveredProviders.add((ThreadContextProvider) instantiate(provider));
        }
        List<ContextManagerExtension> discoveredExtensions = new ArrayList<>();
        for (Class<?> extension : extensionClasses) {
            discoveredExtensions.add((ContextManagerExtension) instantiate(extension));
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

    private static Object instantiate(Class<?> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException("Failed to instantiate declared ThreadContextProvider class: " + clazz.getName(), e);
        }
    }

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

    public static void releaseContextManager() {
        SmallRyeContextManager manager = contextManager;
        if (manager != null) {
            contextManager = null;
            ContextManagerProvider.instance().releaseContextManager(manager);
        }
    }
}
