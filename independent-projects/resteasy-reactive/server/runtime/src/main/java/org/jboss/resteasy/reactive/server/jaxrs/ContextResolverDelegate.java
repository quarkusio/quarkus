package org.jboss.resteasy.reactive.server.jaxrs;

import java.util.Collection;
import javax.ws.rs.ext.ContextResolver;

public class ContextResolverDelegate<T> implements ContextResolver<T> {

    private final Collection<ContextResolver<T>> delegates;

    public ContextResolverDelegate(Collection<ContextResolver<T>> delegates) {
        this.delegates = delegates;
    }

    @Override
    public T getContext(Class<?> type) {
        for (ContextResolver<T> delegate : delegates) {
            T context = delegate.getContext(type);
            if (context != null) {
                return context;
            }
        }
        return null;
    }
}
