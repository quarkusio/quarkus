package io.quarkus.smallrye.context.runtime;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.eclipse.microprofile.context.spi.ContextManagerExtension;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Template;
import io.smallrye.context.SmallRyeContextManager;
import io.smallrye.context.SmallRyeContextManagerProvider;

/**
 * The runtime value service used to create values related to the MP-JWT services
 */
@Template
public class SmallRyeContextPropagationTemplate {

    private static SmallRyeContextManager.Builder builder;

    public void configureStaticInit(List<ThreadContextProvider> discoveredProviders,
            List<ContextManagerExtension> discoveredExtensions) {
        // build the manager at static init time
        ContextManagerProvider contextManagerProvider = new SmallRyeContextManagerProvider();
        ContextManagerProvider.register(contextManagerProvider);
        // do what config we can here, but we need the runtime executor service to finish
        builder = (SmallRyeContextManager.Builder) contextManagerProvider
                .getContextManagerBuilder();
        builder.withThreadContextProviders(discoveredProviders.toArray(new ThreadContextProvider[0]));
        builder.withContextManagerExtensions(discoveredExtensions.toArray(new ContextManagerExtension[0]));
    }

    public void configureRuntime(BeanContainer container, ExecutorService executorService) {
        // associate the static init manager to the runtime CL
        ContextManagerProvider contextManagerProvider = ContextManagerProvider.instance();
        // finish building our manager
        builder.withDefaultExecutorService(executorService);

        SmallRyeContextManager contextManager = builder.build();

        contextManagerProvider.registerContextManager(contextManager, Thread.currentThread().getContextClassLoader());

        // initialise injection
        SmallRyeContextPropagationProvider cpProvider = container.instance(SmallRyeContextPropagationProvider.class);
        cpProvider.initialize(executorService);
    }
}
