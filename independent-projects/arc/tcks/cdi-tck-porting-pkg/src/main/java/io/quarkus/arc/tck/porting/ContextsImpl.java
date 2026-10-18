package io.quarkus.arc.tck.porting;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.Context;

import org.jboss.cdi.tck.spi.Contexts;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;

public class ContextsImpl implements Contexts<Context> {

    // ConcurrentHashMap is just future-proofing, could be implemented with plain map too
    private final Map<Context, InjectableContext.ContextState> contextStateMap = new ConcurrentHashMap<>();

    @Override
    public void setActive(Context context) {
        if (context.isActive()) {
            return;
        }
        if (context instanceof ManagedContext) {
            ManagedContext managed = (ManagedContext) context;
            // remove the context state we potentially stored, else use null to initiate fresh context
            managed.activate(contextStateMap.remove(context));
        }
    }

    @Override
    public void setInactive(Context context) {
        if (!context.isActive()) {
            return;
        }
        if (context instanceof ManagedContext) {
            ManagedContext managed = (ManagedContext) context;
            // save the state of the context
            contextStateMap.put(context, (managed.getState()));
            managed.deactivate();
        }
    }

    @Override
    public Context getRequestContext() {
        return Arc.container().requestContext();
    }

    @Override
    public Context getDependentContext() {
        return Arc.container().getActiveContext(Dependent.class);
    }

    @Override
    public void destroyContext(Context context) {
        ((InjectableContext) context).destroy();
    }
}
