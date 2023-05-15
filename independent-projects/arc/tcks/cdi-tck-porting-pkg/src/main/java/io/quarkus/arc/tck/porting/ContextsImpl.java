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
        // remove the context state we potentially stored, else use null to initiate fresh context
        ((ManagedContext) context).activate(contextStateMap.remove(context));
    }

    @Override
    public void setInactive(Context context) {
        // save the state of the context
        contextStateMap.put(context, ((ManagedContext) context).getState());
        ((ManagedContext) context).deactivate();
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
