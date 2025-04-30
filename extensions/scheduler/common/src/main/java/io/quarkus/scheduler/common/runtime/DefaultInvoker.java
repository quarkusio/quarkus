package io.quarkus.scheduler.common.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext.ContextState;
import io.quarkus.arc.ManagedContext;
import io.quarkus.scheduler.ScheduledExecution;

public abstract class DefaultInvoker implements ScheduledInvoker {

    @Override
    public CompletionStage<Void> invoke(ScheduledExecution execution) throws Exception {
        ManagedContext requestContext = Arc.container().requestContext();
        if (requestContext.isActive()) {
            return invokeBean(execution);
        } else {
            // 1. Activate the context
            // 2. Capture the state (which is basically a shared Map instance)
            // 3. Destroy the context correctly when the returned stage completes
            requestContext.activate();
            final ContextState state = requestContext.getState();
            try {
                return invokeBean(execution).whenComplete((v, t) -> {
                    requestContext.destroy(state);
                });
            } catch (Throwable e) {
                // Terminate the context and return a failed stage if something goes really wrong
                requestContext.terminate();
                return CompletableFuture.failedStage(e);
            } finally {
                // Always deactivate the context
                requestContext.deactivate();
            }
        }
    }

    // This method is generated and should never throw an exception
    protected abstract CompletionStage<Void> invokeBean(ScheduledExecution execution);

}
