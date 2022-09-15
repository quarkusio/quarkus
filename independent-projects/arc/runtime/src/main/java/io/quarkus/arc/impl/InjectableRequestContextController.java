package io.quarkus.arc.impl;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.control.RequestContextController;
import java.util.concurrent.atomic.AtomicBoolean;

@Dependent
public class InjectableRequestContextController implements RequestContextController {

    private final ManagedContext requestContext;
    private final AtomicBoolean isActivator;

    public InjectableRequestContextController() {
        this.requestContext = Arc.container().requestContext();
        this.isActivator = new AtomicBoolean(false);
    }

    @Override
    public boolean activate() {
        if (Arc.container().getActiveContext(RequestScoped.class) != null) {
            return false;
        }
        requestContext.activate();
        isActivator.set(true);
        return true;
    }

    @Override
    public void deactivate() throws ContextNotActiveException {
        if (Arc.container().getActiveContext(RequestScoped.class) == null) {
            throw new ContextNotActiveException(RequestScoped.class.getName());
        }
        if (isActivator.compareAndSet(true, false)) {
            requestContext.terminate();
        }
    }

}
