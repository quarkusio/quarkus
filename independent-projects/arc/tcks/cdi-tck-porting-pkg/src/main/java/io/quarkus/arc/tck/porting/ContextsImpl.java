package io.quarkus.arc.tck.porting;

import java.lang.annotation.Annotation;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

import org.jboss.cdi.tck.spi.Contexts;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;

public class ContextsImpl implements Contexts<Context> {
    @Override
    public void setActive(Context context) {
        ((ManagedContext) context).activate();
    }

    @Override
    public void setInactive(Context context) {
        ((ManagedContext) context).deactivate();
    }

    @Override
    public Context getRequestContext() {
        return Arc.container().requestContext();
    }

    @Override
    public Context getDependentContext() {
        // ArC doesn't have a context object for the dependent context, let's fake it here for now
        // TODO we'll likely have to implement this in ArC properly

        return new Context() {
            @Override
            public Class<? extends Annotation> getScope() {
                return Dependent.class;
            }

            @Override
            public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> T get(Contextual<T> contextual) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isActive() {
                return true;
            }
        };
    }

    @Override
    public void destroyContext(Context context) {
        ((InjectableContext) context).destroy();
    }
}
