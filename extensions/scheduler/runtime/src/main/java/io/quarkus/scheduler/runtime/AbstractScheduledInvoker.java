package io.quarkus.scheduler.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.scheduler.ScheduledExecution;

public abstract class AbstractScheduledInvoker implements ScheduledInvoker {

    @Override
    public void invoke(ScheduledExecution execution) {
        ManagedContext requestContext = Arc.container().requestContext();
        if (requestContext.isActive()) {
            invokeBean(execution);
        } else {
            try {
                requestContext.activate();
                invokeBean(execution);
            } finally {
                requestContext.terminate();
            }
        }
    }

    public abstract void invokeBean(ScheduledExecution action);

}
