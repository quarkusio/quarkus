package io.quarkus.smallrye.context.runtime;

import org.eclipse.microprofile.context.spi.ContextManager;

import io.smallrye.context.SmallRyeContextManager;
import io.smallrye.context.SmallRyeContextManagerProvider;

/**
 * Quarkus doesn't need one manager per CL, we only have the one
 */
public class QuarkusContextManagerProvider extends SmallRyeContextManagerProvider {

    private SmallRyeContextManager contextManager;

    @Override
    public SmallRyeContextManager getContextManager(ClassLoader classLoader) {
        return contextManager;
    }

    @Override
    public SmallRyeContextManager getContextManager() {
        return contextManager;
    }

    @Override
    public ContextManager findContextManager(ClassLoader classLoader) {
        return contextManager;
    }

    @Override
    public void registerContextManager(ContextManager manager, ClassLoader classLoader) {
        if (manager instanceof SmallRyeContextManager == false) {
            throw new IllegalArgumentException("Only instances of SmallRyeContextManager are supported: " + manager);
        }
        contextManager = (SmallRyeContextManager) manager;
    }

    @Override
    public void releaseContextManager(ContextManager manager) {
        contextManager = null;
    }
}
