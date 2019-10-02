package io.quarkus.smallrye.faulttolerance.runtime;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ManagedContext;
import io.smallrye.faulttolerance.CommandListener;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;

@ApplicationScoped
public class RequestContextListener implements CommandListener {
    private final ThreadLocal<Boolean> didActivate;

    public RequestContextListener() {
        this.didActivate = new ThreadLocal<>();
        this.didActivate.set(Boolean.FALSE);
    }

    @Override
    public void beforeExecution(FaultToleranceOperation operation) {
        ArcContainer arc = Arc.container();
        if (arc != null && arc.isRunning()) {
            ManagedContext requestContext = arc.requestContext();
            if (operation.isAsync() && !requestContext.isActive()) {
                requestContext.activate();
                this.didActivate.set(Boolean.TRUE);
            }
        }
    }

    @Override
    public void afterExecution(FaultToleranceOperation operation) {
        if (didActivate.get() == null) {
            // TODO how can this even happen?
            return;
        }
        if (didActivate.get()) {
            ArcContainer arc = Arc.container();
            if (arc != null && arc.isRunning()) {
                ManagedContext requestContext = Arc.container().requestContext();
                requestContext.terminate();
                requestContext.deactivate();
            }
            this.didActivate.set(Boolean.FALSE);
        }
    }
}
