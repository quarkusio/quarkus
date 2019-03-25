package io.quarkus.arc;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.control.RequestContextController;

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
