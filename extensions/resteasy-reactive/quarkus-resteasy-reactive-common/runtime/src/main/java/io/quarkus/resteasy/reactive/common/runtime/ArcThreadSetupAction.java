package io.quarkus.resteasy.reactive.common.runtime;

import org.jboss.resteasy.reactive.common.core.ThreadSetupAction;

import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;

public class ArcThreadSetupAction implements ThreadSetupAction {

    private final ManagedContext managedContext;

    public ArcThreadSetupAction(ManagedContext managedContext) {
        this.managedContext = managedContext;
    }

    @Override
    public ThreadState activateInitial() {
        managedContext.activate();
        InjectableContext.ContextState state = managedContext.getState();
        return new ThreadState() {
            @Override
            public void close() {
                managedContext.destroy(state);
            }

            @Override
            public void activate() {
                managedContext.activate(state);
            }

            @Override
            public void deactivate() {
                managedContext.deactivate();
            }
        };
    }
}
