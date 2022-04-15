package io.quarkus.smallrye.context.runtime;

import java.util.List;
import java.util.concurrent.ExecutorService;
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
import io.smallrye.context.SmallRyeContextManagerProvider;
import io.smallrye.context.SmallRyeManagedExecutor;
import io.smallrye.context.SmallRyeThreadContext;

/**
 * The runtime value service used to create values related to the MP-JWT services
 */
@Recorder
public class SmallRyeContextPropagationRecorder {

    private static SmallRyeContextManager.Builder builder;

    public void configureStaticInit(List<ThreadContextProvider> discoveredProviders,
            List<ContextManagerExtension> discoveredExtensions) {
        // build the manager at static init time
        // in the live-reload mode, the provider instance may be already set in the previous start
        if (ContextManagerProvider.INSTANCE.get() == null) {
            ContextManagerProvider contextManagerProvider = new SmallRyeContextManagerProvider();
            ContextManagerProvider.register(contextManagerProvider);
        }

        // do what config we can here, but we need the runtime executor service to finish
        builder = (SmallRyeContextManager.Builder) ContextManagerProvider.instance()
                .getContextManagerBuilder();
        builder.withThreadContextProviders(discoveredProviders.toArray(new ThreadContextProvider[0]));
        builder.withContextManagerExtensions(discoveredExtensions.toArray(new ContextManagerExtension[0]));
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
        this.builder = null;
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
