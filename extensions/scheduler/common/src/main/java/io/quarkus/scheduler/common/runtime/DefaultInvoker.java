package io.quarkus.scheduler.common.runtime;

import java.util.concurrent.CompletionStage;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.scheduler.ScheduledExecution;

public abstract class DefaultInvoker implements ScheduledInvoker {

    @Override
    public CompletionStage<Void> invoke(ScheduledExecution execution) throws Exception {
        ManagedContext requestContext = Arc.container().requestContext();
        if (requestContext.isActive()) {
            return invokeBean(execution);
        } else {
            try {
                requestContext.activate();
                return invokeBean(execution);
            } finally {
                requestContext.terminate();
            }
        }
    }

    protected abstract CompletionStage<Void> invokeBean(ScheduledExecution execution) throws Exception;

}
