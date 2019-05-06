package io.quarkus.smallrye.context.runtime;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.eclipse.microprofile.context.spi.ContextManager;
import org.eclipse.microprofile.context.spi.ContextManager.Builder;
import org.eclipse.microprofile.context.spi.ContextManagerExtension;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Template;
import io.smallrye.context.SmallRyeContextManagerProvider;

/**
 * The runtime value service used to create values related to the MP-JWT services
 */
@Template
public class SmallRyeContextPropagationTemplate {

    private static ContextManager builtManager;

    public void configureStaticInit(List<ThreadContextProvider> discoveredProviders,
            List<ContextManagerExtension> discoveredExtensions) {
        // build the manager at static init time
        ContextManagerProvider contextManagerProvider = new SmallRyeContextManagerProvider();
        ContextManagerProvider.register(contextManagerProvider);
        Builder builder = contextManagerProvider.getContextManagerBuilder();
        builder.withThreadContextProviders(discoveredProviders.toArray(new ThreadContextProvider[0]));
        builder.withContextManagerExtensions(discoveredExtensions.toArray(new ContextManagerExtension[0]));

        // now store it for runtime
        builtManager = builder.build();
    }

    public void configureRuntime(BeanContainer container, ExecutorService executorService) {
        // associate the static init manager to the runtime CL
        ContextManagerProvider contextManagerProvider = ContextManagerProvider.instance();
        contextManagerProvider.registerContextManager(builtManager, Thread.currentThread().getContextClassLoader());

        // initialise injection
        SmallRyeContextPropagationProvider cpProvider = container.instance(SmallRyeContextPropagationProvider.class);
        cpProvider.initialize(executorService);
    }
}
