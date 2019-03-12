package io.quarkus.undertow.runtime;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ComputingCache;
import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.ContextInstanceHandleImpl;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.InstanceHandle;
import io.undertow.servlet.handlers.ServletRequestContext;

@WebListener
public class HttpSessionContext implements InjectableContext, HttpSessionListener {

    private static final String CONTEXTUAL_INSTANCES_KEY = HttpSessionContext.class.getName()
            + ".contextualInstances";

    @Override
    public Class<? extends Annotation> getScope() {
        return SessionScoped.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        HttpServletRequest request = servletRequest();
        if (request == null) {
            throw new ContextNotActiveException();
        }
        InjectableBean<T> bean = (InjectableBean<T>) contextual;
        ComputingCache<Key, ContextInstanceHandle<?>> contextualInstances = getContextualInstances(request);
        if (creationalContext != null) {
            return (T) contextualInstances.getValue(new Key(creationalContext, bean.getIdentifier())).get();
        } else {
            InstanceHandle<T> handle = (InstanceHandle<T>) contextualInstances
                    .getValueIfPresent(new Key(null, bean.getIdentifier()));
            return handle != null ? handle.get() : null;
        }
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        return get(contextual, null);
    }

    @Override
    public boolean isActive() {
        return servletRequest() != null;
    }

    @Override
    public Collection<ContextInstanceHandle<?>> getAll() {
        HttpServletRequest httpServletRequest = servletRequest();
        if (httpServletRequest != null) {
            return new ArrayList<>(getContextualInstances(httpServletRequest).getPresentValues());
        }
        return Collections.emptyList();
    }

    @Override
    public void destroy(Contextual<?> contextual) {
        HttpServletRequest httpServletRequest = servletRequest();
        if (httpServletRequest == null) {
            throw new ContextNotActiveException();
        }
        InjectableBean<?> bean = (InjectableBean<?>) contextual;
        InstanceHandle<?> instanceHandle = getContextualInstances(httpServletRequest)
                .remove(new Key(null, bean.getIdentifier()));
        if (instanceHandle != null) {
            instanceHandle.destroy();
        }
    }

    @Override
    public void destroy() {
        HttpServletRequest httpServletRequest = servletRequest();
        if (httpServletRequest == null) {
            throw new ContextNotActiveException();
        }
        HttpSession session = httpServletRequest.getSession(false);
        if (session != null) {
            destroy(session);
        }
    }

    private void destroy(HttpSession session) {
        synchronized (this) {
            ComputingCache<Key, ContextInstanceHandle<?>> contextualInstances = getContextualInstances(session);
            for (ContextInstanceHandle<?> instance : contextualInstances.getPresentValues()) {
                try {
                    instance.destroy();
                } catch (Exception e) {
                    throw new IllegalStateException("Unable to destroy instance" + instance.get(), e);
                }
            }
            contextualInstances.clear();
        }
    }

    private ComputingCache<Key, ContextInstanceHandle<?>> getContextualInstances(HttpServletRequest httpServletRequest) {
        return getContextualInstances(httpServletRequest.getSession());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private ComputingCache<Key, ContextInstanceHandle<?>> getContextualInstances(HttpSession session) {
        ComputingCache<Key, ContextInstanceHandle<?>> contextualInstances = (ComputingCache<Key, ContextInstanceHandle<?>>) session
                .getAttribute(CONTEXTUAL_INSTANCES_KEY);
        if (contextualInstances == null) {
            synchronized (this) {
                contextualInstances = (ComputingCache<Key, ContextInstanceHandle<?>>) session
                        .getAttribute(CONTEXTUAL_INSTANCES_KEY);
                if (contextualInstances == null) {
                    contextualInstances = new ComputingCache<>(key -> {
                        InjectableBean bean = Arc.container().bean(key.beanIdentifier);
                        return new ContextInstanceHandleImpl(bean, bean.create(key.creationalContext), key.creationalContext);
                    });
                    session.setAttribute(CONTEXTUAL_INSTANCES_KEY, contextualInstances);
                }
            }
        }
        return contextualInstances;
    }

    private HttpServletRequest servletRequest() {
        try {
            return (HttpServletRequest) ServletRequestContext.requireCurrent().getServletRequest();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    static class Key {

        CreationalContext<?> creationalContext;

        String beanIdentifier;

        public Key(CreationalContext<?> creationalContext, String beanIdentifier) {
            this.creationalContext = creationalContext;
            this.beanIdentifier = beanIdentifier;
        }

        @Override
        public int hashCode() {
            return Objects.hash(beanIdentifier);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Key other = (Key) obj;
            return Objects.equals(beanIdentifier, other.beanIdentifier);
        }

    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        destroy(se.getSession());
    }

}
